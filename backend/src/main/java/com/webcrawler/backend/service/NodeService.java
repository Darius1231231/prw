package com.webcrawler.backend.service;

import com.webcrawler.backend.domain.CrawlerNode;
import com.webcrawler.backend.domain.NodeStatus;
import com.webcrawler.backend.repository.CrawlerNodeRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class NodeService {

    private static final Logger log = LoggerFactory.getLogger(NodeService.class);
    private static final int HEARTBEAT_INTERVAL_MS = 5000;
    private static final int NODE_TIMEOUT_SECONDS = 15;

    private final CrawlerNodeRepository nodeRepository;
    private final UUID currentNodeUuid = UUID.randomUUID();
    private String currentNodeId;

    public NodeService(CrawlerNodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    @PostConstruct
    public void init() {
        try {
            this.currentNodeId = InetAddress.getLocalHost().getHostName() + "-" + currentNodeUuid;
        } catch (UnknownHostException e) {
            this.currentNodeId = "unknown-" + currentNodeUuid;
        }
        registerNode();
    }

    @Transactional
    public void registerNode() {
        CrawlerNode node = new CrawlerNode(currentNodeUuid, currentNodeId, "localhost");
        nodeRepository.save(node);
        log.info("Registered node: {}", currentNodeId);
    }

    @Scheduled(fixedRate = HEARTBEAT_INTERVAL_MS)
    @Transactional
    public void sendHeartbeat() {
        nodeRepository.findById(currentNodeUuid).ifPresentOrElse(node -> {
            node.setLastHeartbeat(LocalDateTime.now());
            node.setStatus(NodeStatus.ACTIVE);
            nodeRepository.save(node);
        }, this::registerNode);
        
        checkLeader();
    }

    @Scheduled(fixedRate = 10000)
    @Transactional
    public void cleanupNodes() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(NODE_TIMEOUT_SECONDS);
        int updated = nodeRepository.markInactiveNodes(threshold);
        if (updated > 0) {
            log.info("Marked {} nodes as OFFLINE", updated);
        }
    }

    @Transactional
    public void checkLeader() {
        // 1. Check if current leader is alive
        Optional<CrawlerNode> currentLeader = nodeRepository.findFirstByIsLeaderTrue();
        if (currentLeader.isPresent()) {
            CrawlerNode leader = currentLeader.get();
            if (leader.getLastHeartbeat().isAfter(LocalDateTime.now().minusSeconds(NODE_TIMEOUT_SECONDS))) {
                // Leader is healthy
                return;
            }
            // Leader is dead, demote
            leader.setLeader(false);
            nodeRepository.save(leader);
            log.info("Leader {} is dead. Initiating election.", leader.getNodeId());
        }

        // 2. Elect new leader (Oldest active node)
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(NODE_TIMEOUT_SECONDS);
        List<CrawlerNode> activeNodes = nodeRepository.findActiveNodesOrderedByHeartbeat(threshold);
        if (!activeNodes.isEmpty()) {
            CrawlerNode newLeader = activeNodes.get(0);
            newLeader.setLeader(true);
            nodeRepository.save(newLeader);
            log.info("Node {} elected as new leader", newLeader.getNodeId());
        }
    }

    public boolean isLeader() {
        return nodeRepository.findById(currentNodeUuid)
            .map(CrawlerNode::isLeader)
            .orElse(false);
    }
    
    public UUID getCurrentNodeId() {
        return currentNodeUuid;
    }
}
