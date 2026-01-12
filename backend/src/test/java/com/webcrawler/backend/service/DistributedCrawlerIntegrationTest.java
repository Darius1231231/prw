package com.webcrawler.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import com.webcrawler.backend.domain.AppUser;
import com.webcrawler.backend.domain.CrawlJob;
import com.webcrawler.backend.domain.CrawlStatus;
import com.webcrawler.backend.domain.CrawlerNode;
import com.webcrawler.backend.domain.NodeStatus;
import com.webcrawler.backend.repository.AppUserRepository;
import com.webcrawler.backend.repository.CrawlJobRepository;
import com.webcrawler.backend.repository.CrawlResultRepository;
import com.webcrawler.backend.repository.CrawlerNodeRepository;
import com.webcrawler.backend.web.dto.CrawlRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = {"crawler.node.auto-register=false"})
public class DistributedCrawlerIntegrationTest {

    @Autowired
    private CrawlerService crawlerService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private CrawlJobRepository crawlJobRepository;

    @Autowired
    private CrawlerNodeRepository crawlerNodeRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CrawlResultRepository crawlResultRepository;

    private AppUser testUser;

    @BeforeEach
    public void setUp() {
        // Clean up previous test data
        crawlResultRepository.deleteAll();
        crawlJobRepository.deleteAll();
        crawlerNodeRepository.deleteAll();
        appUserRepository.deleteAll();

        // Create test user
        testUser = new AppUser();
        testUser.setUsername("testuser_" + UUID.randomUUID());
        testUser.setPassword("password");
        appUserRepository.save(testUser);
    }

    @Test
    public void testNodeRegistration() {
        // When a node is registered
        CrawlerNode node = new CrawlerNode(UUID.randomUUID(), "test-node-1", "localhost");
        node.setStatus(NodeStatus.ACTIVE);
        crawlerNodeRepository.save(node);

        // Then it should be retrievable
        List<CrawlerNode> nodes = crawlerNodeRepository.findAll();
        assertFalse(nodes.isEmpty(), "Node should be registered");
        assertTrue(nodes.stream().anyMatch(n -> n.getNodeId().equals("test-node-1")));
    }

    @Test
    public void testMultipleNodesRegistration() {
        // When multiple nodes are registered
        for (int i = 0; i < 3; i++) {
            CrawlerNode node = new CrawlerNode(UUID.randomUUID(), "crawler-node-" + i, "localhost:800" + i);
            node.setStatus(NodeStatus.ACTIVE);
            crawlerNodeRepository.save(node);
        }

        // Then all should be retrievable
        List<CrawlerNode> nodes = crawlerNodeRepository.findAll();
        assertEquals(3, nodes.size(), "All 3 nodes should be registered");
        assertTrue(nodes.stream().allMatch(n -> n.getStatus() == NodeStatus.ACTIVE));
    }

    @Test
    public void testCrawlJobCreation() {
        // When a crawl job is created
        CrawlRequest request = new CrawlRequest();
        request.setUrl("https://example.com");
        request.setMaxDepth(2);
        request.setMaxPages(100);

        CrawlJob job = crawlerService.startCrawl(request, testUser);

        // Then job should be created with correct properties
        assertNotNull(job.getId());
        assertEquals("https://example.com", job.getSeedUrl());
        assertEquals(2, job.getMaxDepth());
        assertEquals(100, job.getMaxPages());
        assertEquals(CrawlStatus.PENDING, job.getStatus());
        assertEquals(testUser.getUsername(), job.getOwner().getUsername());
    }

    @Test
    public void testCrawlJobAssignment() {
        // Given multiple crawler nodes
        CrawlerNode node1 = new CrawlerNode(UUID.randomUUID(), "node-1", "localhost:8001");
        node1.setStatus(NodeStatus.ACTIVE);
        CrawlerNode node2 = new CrawlerNode(UUID.randomUUID(), "node-2", "localhost:8002");
        node2.setStatus(NodeStatus.ACTIVE);
        crawlerNodeRepository.saveAll(List.of(node1, node2));

        // When a crawl job is created
        CrawlRequest request = new CrawlRequest();
        request.setUrl("https://example.com");
        request.setMaxDepth(1);
        request.setMaxPages(50);
        CrawlJob job = crawlerService.startCrawl(request, testUser);

        // Then the job should exist
        assertTrue(crawlJobRepository.existsById(job.getId()));
        assertEquals(CrawlStatus.PENDING, job.getStatus());
    }

    @Test
    public void testNodeHeartbeat() {
        // When a node is active
        CrawlerNode node = new CrawlerNode(UUID.randomUUID(), "active-node", "localhost");
        node.setStatus(NodeStatus.ACTIVE);
        LocalDateTime beforeSave = LocalDateTime.now();
        crawlerNodeRepository.save(node);
        LocalDateTime afterSave = LocalDateTime.now();

        // Then last heartbeat should be recorded
        CrawlerNode retrievedNode = crawlerNodeRepository.findById(node.getId()).orElse(null);
        assertNotNull(retrievedNode);
        assertNotNull(retrievedNode.getLastHeartbeat());
        assertTrue(retrievedNode.getLastHeartbeat().isAfter(beforeSave.minusSeconds(1)));
        assertTrue(retrievedNode.getLastHeartbeat().isBefore(afterSave.plusSeconds(1)));
    }

    @Test
    public void testNodeStatusChange() {
        // Given an active node
        CrawlerNode node = new CrawlerNode(UUID.randomUUID(), "status-node", "localhost");
        node.setStatus(NodeStatus.ACTIVE);
        crawlerNodeRepository.save(node);

        // When status is changed to INACTIVE
        node.setStatus(NodeStatus.INACTIVE);
        crawlerNodeRepository.save(node);

        // Then the status should be updated
        CrawlerNode updatedNode = crawlerNodeRepository.findById(node.getId()).orElse(null);
        assertNotNull(updatedNode);
        assertEquals(NodeStatus.INACTIVE, updatedNode.getStatus());
    }

    @Test
    public void testMultipleCrawlJobsPerNode() {
        // Given a crawler node
        CrawlerNode node = new CrawlerNode(UUID.randomUUID(), "multi-job-node", "localhost");
        node.setStatus(NodeStatus.ACTIVE);
        crawlerNodeRepository.save(node);

        // When multiple crawl jobs are created by the same user
        CrawlRequest request1 = new CrawlRequest();
        request1.setUrl("https://example1.com");
        request1.setMaxDepth(1);
        request1.setMaxPages(10);

        CrawlRequest request2 = new CrawlRequest();
        request2.setUrl("https://example2.com");
        request2.setMaxDepth(2);
        request2.setMaxPages(20);

        CrawlJob job1 = crawlerService.startCrawl(request1, testUser);
        CrawlJob job2 = crawlerService.startCrawl(request2, testUser);

        // Then both jobs should be created independently
        assertNotEquals(job1.getId(), job2.getId());
        assertEquals(CrawlStatus.PENDING, job1.getStatus());
        assertEquals(CrawlStatus.PENDING, job2.getStatus());
    }

    @Test
    public void testUserJobIsolation() {
        // Given two different users
        AppUser user2 = new AppUser();
        user2.setUsername("testuser_" + UUID.randomUUID());
        user2.setPassword("password");
        appUserRepository.save(user2);

        // When each user creates a crawl job
        CrawlRequest request = new CrawlRequest();
        request.setUrl("https://example.com");
        request.setMaxDepth(1);
        request.setMaxPages(10);

        CrawlJob job1 = crawlerService.startCrawl(request, testUser);
        CrawlJob job2 = crawlerService.startCrawl(request, user2);

        // Then each user should only see their own jobs
        List<CrawlJob> user1Jobs = crawlerService.listJobs(testUser);
        List<CrawlJob> user2Jobs = crawlerService.listJobs(user2);

        assertEquals(1, user1Jobs.size());
        assertEquals(1, user2Jobs.size());
        assertEquals(job1.getId(), user1Jobs.get(0).getId());
        assertEquals(job2.getId(), user2Jobs.get(0).getId());
    }

    @Test
    public void testNodeDiscovery() {
        // When multiple nodes are registered
        for (int i = 0; i < 5; i++) {
            CrawlerNode node = new CrawlerNode(UUID.randomUUID(), "discovery-node-" + i, "localhost:900" + i);
            node.setStatus(NodeStatus.ACTIVE);
            crawlerNodeRepository.save(node);
        }

        // Then all nodes should be discoverable
        List<CrawlerNode> allNodes = crawlerNodeRepository.findAll();
        assertEquals(5, allNodes.size(), "All 5 nodes should be discoverable");
        assertTrue(allNodes.stream().allMatch(n -> n.getStatus() == NodeStatus.ACTIVE));
    }
}
