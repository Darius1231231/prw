package com.webcrawler.backend.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.webcrawler.backend.domain.CrawlResult;
import java.time.LocalDateTime;
import java.util.UUID;

public class CrawlResultResponse {

    private final UUID id;
    private final String url;
    private final int statusCode;
    private final String title;
    private final int linkCount;
    private final String errorMessage;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final LocalDateTime crawledAt;

    public CrawlResultResponse(CrawlResult result) {
        this.id = result.getId();
        this.url = result.getUrl();
        this.statusCode = result.getStatusCode();
        this.title = result.getTitle();
        this.linkCount = result.getLinkCount();
        this.errorMessage = result.getErrorMessage();
        this.crawledAt = result.getCrawledAt();
    }

    public UUID getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getTitle() {
        return title;
    }

    public int getLinkCount() {
        return linkCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getCrawledAt() {
        return crawledAt;
    }
}
