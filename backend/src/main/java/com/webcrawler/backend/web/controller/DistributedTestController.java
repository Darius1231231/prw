package com.webcrawler.backend.web.controller;

import com.webcrawler.backend.domain.CrawlerNode;
import com.webcrawler.backend.domain.NodeStatus;
import com.webcrawler.backend.repository.CrawlerNodeRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for testing and monitoring distributed crawler nodes.
 * Provides endpoints to inspect and manage node status in the cluster.
 */
@RestController
@RequestMapping("/api/test/nodes")
public class DistributedTestController {

    private final CrawlerNodeRepository crawlerNodeRepository;

    public DistributedTestController(CrawlerNodeRepository crawlerNodeRepository) {
        this.crawlerNodeRepository = crawlerNodeRepository;
    }

    /**
     * Get all registered crawler nodes in the cluster
     */
    @GetMapping
    public ResponseEntity<List<CrawlerNode>> getAllNodes() {
        List<CrawlerNode> nodes = crawlerNodeRepository.findAll();
        return ResponseEntity.ok(nodes);
    }

    /**
     * Get only active nodes
     */
    @GetMapping("/active")
    public ResponseEntity<List<CrawlerNode>> getActiveNodes() {
        List<CrawlerNode> activeNodes = crawlerNodeRepository.findAll()
            .stream()
            .filter(n -> n.getStatus() == NodeStatus.ACTIVE)
            .toList();
        return ResponseEntity.ok(activeNodes);
    }

    /**
     * Get cluster statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getClusterStats() {
        List<CrawlerNode> allNodes = crawlerNodeRepository.findAll();
        long activeCount = allNodes.stream()
            .filter(n -> n.getStatus() == NodeStatus.ACTIVE)
            .count();

        return ResponseEntity.ok(Map.of(
            "totalNodes", allNodes.size(),
            "activeNodes", activeCount,
            "inactiveNodes", allNodes.size() - activeCount,
            "timestamp", LocalDateTime.now()
        ));
    }

    /**
     * Manually register a new crawler node
     */
    @PostMapping("/register")
    public ResponseEntity<CrawlerNode> registerNode(@RequestBody NodeRegistrationRequest request) {
        CrawlerNode node = new CrawlerNode(
            UUID.randomUUID(),
            request.getNodeId(),
            request.getHostname()
        );
        node.setStatus(NodeStatus.ACTIVE);
        CrawlerNode savedNode = crawlerNodeRepository.save(node);
        return ResponseEntity.ok(savedNode);
    }

    /**
     * Simulate a node heartbeat
     */
    @PostMapping("/heartbeat")
    public ResponseEntity<CrawlerNode> sendHeartbeat(@RequestBody HeartbeatRequest request) {
        return crawlerNodeRepository.findById(request.getNodeId())
            .map(node -> {
                node.setLastHeartbeat(LocalDateTime.now());
                node.setStatus(NodeStatus.ACTIVE);
                return ResponseEntity.ok(crawlerNodeRepository.save(node));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Reset all nodes for testing
     */
    @PostMapping("/reset")
    public ResponseEntity<String> resetAllNodes() {
        crawlerNodeRepository.deleteAll();
        return ResponseEntity.ok("All nodes reset successfully");
    }

    /**
     * DTO for node registration
     */
    public static class NodeRegistrationRequest {
        private String nodeId;
        private String hostname;

        public NodeRegistrationRequest() {}

        public NodeRegistrationRequest(String nodeId, String hostname) {
            this.nodeId = nodeId;
            this.hostname = hostname;
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }
    }

    /**
     * DTO for heartbeat
     */
    public static class HeartbeatRequest {
        private UUID nodeId;

        public HeartbeatRequest() {}

        public HeartbeatRequest(UUID nodeId) {
            this.nodeId = nodeId;
        }

        public UUID getNodeId() {
            return nodeId;
        }

        public void setNodeId(UUID nodeId) {
            this.nodeId = nodeId;
        }
    }
}
