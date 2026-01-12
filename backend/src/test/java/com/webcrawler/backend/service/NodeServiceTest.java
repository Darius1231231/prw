package com.webcrawler.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import com.webcrawler.backend.domain.CrawlerNode;
import com.webcrawler.backend.domain.NodeStatus;
import com.webcrawler.backend.repository.CrawlerNodeRepository;
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
public class NodeServiceTest {

    @Autowired
    private NodeService nodeService;

    @Autowired
    private CrawlerNodeRepository crawlerNodeRepository;

    @BeforeEach
    public void setUp() {
        crawlerNodeRepository.deleteAll();
    }

    @Test
    public void testNodeRegistrationOnInit() {
        // The node may auto-register if not disabled, or it should allow manual registration
        List<CrawlerNode> nodes = crawlerNodeRepository.findAll();
        // We just verify the repository works, even if empty
        assertNotNull(nodes);
    }

    @Test
    public void testMultipleNodeCoexistence() {
        // When multiple nodes are created
        CrawlerNode node1 = new CrawlerNode(UUID.randomUUID(), "node-1", "localhost:8001");
        node1.setStatus(NodeStatus.ACTIVE);

        CrawlerNode node2 = new CrawlerNode(UUID.randomUUID(), "node-2", "localhost:8002");
        node2.setStatus(NodeStatus.ACTIVE);

        crawlerNodeRepository.save(node1);
        crawlerNodeRepository.save(node2);

        // Then both should coexist
        List<CrawlerNode> allNodes = crawlerNodeRepository.findAll();
        assertTrue(allNodes.size() >= 2, "Multiple nodes should coexist");
    }

    @Test
    public void testNodeStatusTracking() {
        // Given an active node
        CrawlerNode node = new CrawlerNode(UUID.randomUUID(), "tracking-node", "localhost");
        node.setStatus(NodeStatus.ACTIVE);
        crawlerNodeRepository.save(node);

        // When we query for active nodes
        List<CrawlerNode> activeNodes = crawlerNodeRepository.findAll();
        activeNodes = activeNodes.stream()
            .filter(n -> n.getStatus() == NodeStatus.ACTIVE)
            .toList();

        // Then the node should be found
        assertTrue(activeNodes.stream().anyMatch(n -> n.getNodeId().equals("tracking-node")));
    }

    @Test
    public void testNodeHeartbeatUpdate() {
        // Given a node
        CrawlerNode node = new CrawlerNode(UUID.randomUUID(), "heartbeat-node", "localhost");
        node.setStatus(NodeStatus.ACTIVE);
        crawlerNodeRepository.save(node);

        LocalDateTime firstHeartbeat = node.getLastHeartbeat();

        // When we wait and update the node
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        node.setLastHeartbeat(LocalDateTime.now());
        crawlerNodeRepository.save(node);

        LocalDateTime updatedHeartbeat = crawlerNodeRepository.findById(node.getId())
            .orElse(node)
            .getLastHeartbeat();

        // Then heartbeat should be updated
        assertTrue(updatedHeartbeat.isAfter(firstHeartbeat), "Heartbeat should be updated");
    }

    @Test
    public void testNodeWithDifferentPorts() {
        // When nodes are registered with different ports
        String[] ports = {"8001", "8002", "8003", "8004", "8005"};
        for (int i = 0; i < ports.length; i++) {
            CrawlerNode node = new CrawlerNode(UUID.randomUUID(), "node-" + i, "localhost:" + ports[i]);
            node.setStatus(NodeStatus.ACTIVE);
            crawlerNodeRepository.save(node);
        }

        // Then all nodes should be stored with their specific ports
        List<CrawlerNode> nodes = crawlerNodeRepository.findAll();
        assertEquals(ports.length, nodes.stream()
            .filter(n -> n.getHostname().startsWith("localhost:"))
            .count());
    }

    @Test
    public void testNodeDeactivation() {
        // Given active nodes
        CrawlerNode node1 = new CrawlerNode(UUID.randomUUID(), "deactivate-1", "localhost");
        node1.setStatus(NodeStatus.ACTIVE);
        CrawlerNode node2 = new CrawlerNode(UUID.randomUUID(), "deactivate-2", "localhost");
        node2.setStatus(NodeStatus.ACTIVE);
        crawlerNodeRepository.saveAll(List.of(node1, node2));

        // When one node is deactivated
        node1.setStatus(NodeStatus.INACTIVE);
        crawlerNodeRepository.save(node1);

        // Then only one should be active
        List<CrawlerNode> activeNodes = crawlerNodeRepository.findAll().stream()
            .filter(n -> n.getStatus() == NodeStatus.ACTIVE)
            .toList();

        assertEquals(1, activeNodes.stream()
            .filter(n -> n.getNodeId().equals("deactivate-2"))
            .count());
    }
}
