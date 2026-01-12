package com.webcrawler.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "crawl_tasks", indexes = {
    @Index(name = "idx_task_job_id", columnList = "job_id"),
    @Index(name = "idx_task_assigned_node", columnList = "assigned_node_id, status")
})
public class CrawlTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private CrawlJob job;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(nullable = false)
    private int depth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    private UUID assignedNodeId;

    private LocalDateTime createdAt;
    private LocalDateTime assignedAt;
    private LocalDateTime completedAt;

    public CrawlTask() {
    }

    public CrawlTask(CrawlJob job, String url, int depth) {
        this.job = job;
        this.url = url;
        this.depth = depth;
        this.status = TaskStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public CrawlJob getJob() {
        return job;
    }

    public void setJob(CrawlJob job) {
        this.job = job;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public UUID getAssignedNodeId() {
        return assignedNodeId;
    }

    public void setAssignedNodeId(UUID assignedNodeId) {
        this.assignedNodeId = assignedNodeId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
