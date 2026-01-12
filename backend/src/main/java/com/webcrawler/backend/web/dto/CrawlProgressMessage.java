package com.webcrawler.backend.web.dto;

import com.webcrawler.backend.domain.CrawlStatus;
import java.util.UUID;

public class CrawlProgressMessage {

    private final UUID jobId;
    private final String url;
    private final CrawlStatus status;
    private final int visitedCount;
    private final int pendingCount;
    private final String message;

    public CrawlProgressMessage(UUID jobId, String url, CrawlStatus status, int visitedCount, int pendingCount, String message) {
        this.jobId = jobId;
        this.url = url;
        this.status = status;
        this.visitedCount = visitedCount;
        this.pendingCount = pendingCount;
        this.message = message;
    }

    public UUID getJobId() {
        return jobId;
    }

    public String getUrl() {
        return url;
    }

    public CrawlStatus getStatus() {
        return status;
    }

    public int getVisitedCount() {
        return visitedCount;
    }

    public int getPendingCount() {
        return pendingCount;
    }

    public String getMessage() {
        return message;
    }
}
