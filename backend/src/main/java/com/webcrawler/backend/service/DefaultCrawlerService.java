package com.webcrawler.backend.service;

import com.webcrawler.backend.domain.AppUser;
import com.webcrawler.backend.domain.CrawlJob;
import com.webcrawler.backend.domain.CrawlResult;
import com.webcrawler.backend.domain.CrawlStatus;
import com.webcrawler.backend.domain.CrawlTask;
import com.webcrawler.backend.domain.CrawlerNode;
import com.webcrawler.backend.domain.TaskStatus;
import com.webcrawler.backend.repository.CrawlJobRepository;
import com.webcrawler.backend.repository.CrawlResultRepository;
import com.webcrawler.backend.repository.CrawlTaskRepository;
import com.webcrawler.backend.repository.CrawlerNodeRepository;
import com.webcrawler.backend.web.dto.CrawlProgressMessage;
import com.webcrawler.backend.web.dto.CrawlRequest;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class DefaultCrawlerService implements CrawlerService {

    private static final Logger log = LoggerFactory.getLogger(DefaultCrawlerService.class);

    private final CrawlJobRepository crawlJobRepository;
    private final CrawlResultRepository crawlResultRepository;
    private final CrawlTaskRepository crawlTaskRepository;
    private final CrawlerNodeRepository crawlerNodeRepository;
    private final NodeService nodeService;
    private final SimpMessagingTemplate messagingTemplate;
    private final HttpClient httpClient;

    public DefaultCrawlerService(
        CrawlJobRepository crawlJobRepository,
        CrawlResultRepository crawlResultRepository,
        CrawlTaskRepository crawlTaskRepository,
        CrawlerNodeRepository crawlerNodeRepository,
        NodeService nodeService,
        SimpMessagingTemplate messagingTemplate
    ) {
        this.crawlJobRepository = crawlJobRepository;
        this.crawlResultRepository = crawlResultRepository;
        this.crawlTaskRepository = crawlTaskRepository;
        this.crawlerNodeRepository = crawlerNodeRepository;
        this.nodeService = nodeService;
        this.messagingTemplate = messagingTemplate;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    @Override
    @Transactional
    public CrawlJob startCrawl(CrawlRequest request, AppUser owner) {
        CrawlJob job = new CrawlJob();
        job.setSeedUrl(normalizeUrl(request.getUrl()));
        job.setMaxDepth(request.getMaxDepth());
        job.setMaxPages(request.getMaxPages());
        job.setOwner(owner);
        job.setStatus(CrawlStatus.PENDING);
        crawlJobRepository.save(job);

        // Create initial task
        CrawlTask seedTask = new CrawlTask(job, job.getSeedUrl(), 0);
        crawlTaskRepository.save(seedTask);

        sendStatus(job, 0, 0, "Crawl job queued");
        return job;
    }

    @Override
    public List<CrawlJob> listJobs(AppUser owner) {
        return crawlJobRepository.findByOwnerUsernameOrderByCreatedAtDesc(owner.getUsername());
    }

    @Override
    public Optional<CrawlJob> getJob(UUID jobId, AppUser owner) {
        return crawlJobRepository.findWithResultsById(jobId)
            .filter(job -> job.getOwner() != null && job.getOwner().getId().equals(owner.getId()));
    }

    // Leader Task: Assign PENDING tasks to nodes
    @Scheduled(fixedRate = 2000)
    @Transactional
    public void assignTasks() {
        if (!nodeService.isLeader()) {
            return;
        }

        List<CrawlTask> pendingTasks = crawlTaskRepository.findByStatus(TaskStatus.PENDING, PageRequest.of(0, 50));
        if (pendingTasks.isEmpty()) {
            return;
        }
        
        log.info("Found {} pending tasks. Assigning to active nodes...", pendingTasks.size());

        LocalDateTime threshold = LocalDateTime.now().minusSeconds(15); // Hardcoded timeout matching NodeService
        List<CrawlerNode> activeNodes = crawlerNodeRepository.findActiveNodesOrderedByHeartbeat(threshold);
        if (activeNodes.isEmpty()) {
            log.warn("No active nodes found to assign tasks!");
            return;
        }

        int nodeIndex = 0;
        for (CrawlTask task : pendingTasks) {
            CrawlerNode node = activeNodes.get(nodeIndex);
            task.setAssignedNodeId(node.getId());
            task.setStatus(TaskStatus.ASSIGNED);
            task.setAssignedAt(LocalDateTime.now());
            crawlTaskRepository.save(task);
            
            if (task.getJob().getStatus() == CrawlStatus.PENDING) {
                task.getJob().setStatus(CrawlStatus.RUNNING);
                task.getJob().setStartedAt(LocalDateTime.now());
                crawlJobRepository.save(task.getJob());
            }

            nodeIndex = (nodeIndex + 1) % activeNodes.size();
        }
    }

    // Worker Task: Process ASSIGNED tasks
    @Scheduled(fixedRate = 1000)
    @Transactional
    public void processTasks() {
        UUID myNodeId = nodeService.getCurrentNodeId();
        List<CrawlTask> myTasks = crawlTaskRepository.findByAssignedNodeIdAndStatus(myNodeId, TaskStatus.ASSIGNED);

        for (CrawlTask task : myTasks) {
            processTask(task);
        }
    }

    private void processTask(CrawlTask task) {
        CrawlJob job = task.getJob();
        
        long totalProcessed = crawlTaskRepository.countCompletedByJobId(job.getId());
        if (totalProcessed >= job.getMaxPages()) {
            task.setStatus(TaskStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            crawlTaskRepository.save(task);
            return;
        }

        try {
            PageProcessingResult pageResult = fetchAndProcess(job, task.getUrl());
            crawlResultRepository.save(pageResult.result());

            if (task.getDepth() < job.getMaxDepth()) {
                createChildTasks(job, task.getDepth(), pageResult.links());
            }

            task.setStatus(TaskStatus.COMPLETED);
            sendStatus(job, (int)totalProcessed + 1, 0, "Processed " + task.getUrl());
        } catch (Exception ex) {
            log.warn("Failed to crawl {}", task.getUrl(), ex);
            CrawlResult failedResult = buildFailedResult(job, task.getUrl(), ex.getMessage());
            crawlResultRepository.save(failedResult);
            task.setStatus(TaskStatus.FAILED);
        } finally {
            task.setCompletedAt(LocalDateTime.now());
            crawlTaskRepository.save(task);
        }
    }

    private void createChildTasks(CrawlJob job, int currentDepth, List<String> links) {
        List<String> filtered = links.stream()
            .filter(url -> url.startsWith("http"))
            .distinct()
            .collect(Collectors.toList());

        for (String link : filtered) {
            if (!crawlTaskRepository.existsByJobIdAndUrl(job.getId(), link)) {
                long totalTasks = crawlTaskRepository.countTotalByJobId(job.getId());
                if (totalTasks >= job.getMaxPages()) {
                    break;
                }
                
                CrawlTask newTask = new CrawlTask(job, link, currentDepth + 1);
                crawlTaskRepository.save(newTask);
            }
        }
    }

    private PageProcessingResult fetchAndProcess(CrawlJob job, String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .GET()
            .timeout(Duration.ofSeconds(15))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        Document document = Jsoup.parse(response.body(), url);
        Elements linkElements = document.select("a[href]");
        int linkCount = linkElements.size();
        String title = document.title();

        CrawlResult result = new CrawlResult();
        result.setJob(job);
        result.setUrl(url);
        result.setStatusCode(response.statusCode());
        result.setTitle(title);
        result.setLinkCount(linkCount);
        if (response.statusCode() >= 400) {
            result.setErrorMessage("HTTP " + response.statusCode());
        }
        result.setCrawledAt(LocalDateTime.now());

        List<String> links = linkElements.stream()
            .map(element -> element.attr("abs:href"))
            .map(DefaultCrawlerService::normalizeUrl)
            .collect(Collectors.toList());

        return new PageProcessingResult(result, links);
    }

    private CrawlResult buildFailedResult(CrawlJob job, String url, String message) {
        CrawlResult result = new CrawlResult();
        result.setJob(job);
        result.setUrl(url);
        result.setStatusCode(0);
        result.setLinkCount(0);
        result.setErrorMessage(message);
        result.setCrawledAt(LocalDateTime.now());
        return result;
    }

    private void sendStatus(CrawlJob job, int processed, int pending, String message) {
        CrawlProgressMessage progressMessage = new CrawlProgressMessage(
            job.getId(),
            job.getSeedUrl(),
            job.getStatus(),
            processed,
            pending,
            message
        );
        messagingTemplate.convertAndSend("/topic/crawls", progressMessage);
        messagingTemplate.convertAndSend("/topic/crawls/" + job.getId(), progressMessage);
    }

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void checkJobCompletion() {
        if (!nodeService.isLeader()) {
            return;
        }
        
        List<CrawlJob> runningJobs = crawlJobRepository.findByStatus(CrawlStatus.RUNNING);
        for (CrawlJob job : runningJobs) {
            long completed = crawlTaskRepository.countCompletedByJobId(job.getId());
            if (completed >= job.getMaxPages()) {
                job.setStatus(CrawlStatus.COMPLETED);
                job.setCompletedAt(LocalDateTime.now());
                crawlJobRepository.save(job);
                sendStatus(job, (int)completed, 0, "Crawl completed");
            }
        }
    }

    private static String normalizeUrl(String rawUrl) {
        try {
            return URI.create(rawUrl).normalize().toString();
        } catch (Exception ex) {
            return rawUrl;
        }
    }

    private record PageProcessingResult(CrawlResult result, List<String> links) {
    }
}
