package com.webcrawler.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "crawler_nodes")
public class CrawlerNode {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String nodeId;

    @Column(nullable = false)
    private String hostname;

    @Column(nullable = false)
    private LocalDateTime lastHeartbeat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NodeStatus status;

    private boolean isLeader;

    public CrawlerNode() {
    }

    public CrawlerNode(UUID id, String nodeId, String hostname) {
        this.id = id;
        this.nodeId = nodeId;
        this.hostname = hostname;
        this.lastHeartbeat = LocalDateTime.now();
        this.status = NodeStatus.ACTIVE;
        this.isLeader = false;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public LocalDateTime getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(LocalDateTime lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public NodeStatus getStatus() {
        return status;
    }

    public void setStatus(NodeStatus status) {
        this.status = status;
    }

    public boolean isLeader() {
        return isLeader;
    }

    public void setLeader(boolean leader) {
        isLeader = leader;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CrawlerNode that = (CrawlerNode) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
