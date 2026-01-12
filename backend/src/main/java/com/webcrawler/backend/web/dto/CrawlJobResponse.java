package com.webcrawler.backend.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.webcrawler.backend.domain.CrawlJob;
import com.webcrawler.backend.domain.CrawlStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class CrawlJobResponse {

    private final UUID id;
    private final String seedUrl;
    private final int maxDepth;
    private final int maxPages;
    private final CrawlStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final LocalDateTime startedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final LocalDateTime completedAt;

    private final List<CrawlResultResponse> results;

    public CrawlJobResponse(CrawlJob job, List<CrawlResultResponse> results) {
        this.id = job.getId();
        this.seedUrl = job.getSeedUrl();
        this.maxDepth = job.getMaxDepth();
        this.maxPages = job.getMaxPages();
        this.status = job.getStatus();
        this.createdAt = job.getCreatedAt();
        this.startedAt = job.getStartedAt();
        this.completedAt = job.getCompletedAt();
        this.results = results;
    }

    public UUID getId() {
        return id;
    }

    public String getSeedUrl() {
        return seedUrl;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public CrawlStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public List<CrawlResultResponse> getResults() {
        return results;
    }
}
