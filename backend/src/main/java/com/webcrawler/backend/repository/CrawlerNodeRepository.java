package com.webcrawler.backend.repository;

import com.webcrawler.backend.domain.CrawlerNode;
import com.webcrawler.backend.domain.NodeStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CrawlerNodeRepository extends JpaRepository<CrawlerNode, UUID> {
    Optional<CrawlerNode> findByNodeId(String nodeId);
    
    List<CrawlerNode> findByStatus(NodeStatus status);

    @Query("SELECT n FROM CrawlerNode n WHERE n.status = 'ACTIVE' AND n.lastHeartbeat > :threshold ORDER BY n.lastHeartbeat ASC")
    List<CrawlerNode> findActiveNodesOrderedByHeartbeat(LocalDateTime threshold);

    @Modifying
    @Query("UPDATE CrawlerNode n SET n.status = 'OFFLINE', n.isLeader = false WHERE n.lastHeartbeat < :threshold AND n.status = 'ACTIVE'")
    int markInactiveNodes(LocalDateTime threshold);
    
    Optional<CrawlerNode> findFirstByIsLeaderTrue();
}
