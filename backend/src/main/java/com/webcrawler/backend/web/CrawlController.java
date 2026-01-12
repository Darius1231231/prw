package com.webcrawler.backend.web;

import com.webcrawler.backend.domain.AppUser;
import com.webcrawler.backend.domain.CrawlJob;
import com.webcrawler.backend.repository.CrawlResultRepository;
import com.webcrawler.backend.service.CrawlerService;
import com.webcrawler.backend.web.dto.CrawlJobResponse;
import com.webcrawler.backend.web.dto.CrawlRequest;
import com.webcrawler.backend.web.dto.CrawlResultResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crawls")
public class CrawlController {

    private final CrawlerService crawlerService;
    private final CrawlResultRepository crawlResultRepository;

    public CrawlController(CrawlerService crawlerService, CrawlResultRepository crawlResultRepository) {
        this.crawlerService = crawlerService;
        this.crawlResultRepository = crawlResultRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CrawlJobResponse> createCrawl(@Valid @RequestBody CrawlRequest request, Authentication authentication) {
        AppUser user = (AppUser) authentication.getPrincipal();
        CrawlJob job = crawlerService.startCrawl(request, user);
        return ResponseEntity.accepted().body(toResponse(job, List.of()));
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public List<CrawlJobResponse> listCrawls(Authentication authentication) {
        AppUser user = (AppUser) authentication.getPrincipal();
        return crawlerService.listJobs(user).stream()
            .map(job -> toResponse(job, List.of()))
            .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CrawlJobResponse> getCrawl(@PathVariable UUID id, Authentication authentication) {
        AppUser user = (AppUser) authentication.getPrincipal();
        return crawlerService.getJob(id, user)
            .map(job -> {
                List<CrawlResultResponse> results = crawlResultRepository.findByJobIdOrderByCrawledAtAsc(job.getId()).stream()
                    .map(CrawlResultResponse::new)
                    .toList();
                return ResponseEntity.ok(toResponse(job, results));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private CrawlJobResponse toResponse(CrawlJob job, List<CrawlResultResponse> results) {
        return new CrawlJobResponse(job, results);
    }
}
