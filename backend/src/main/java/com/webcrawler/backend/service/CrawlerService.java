package com.webcrawler.backend.service;

import com.webcrawler.backend.domain.AppUser;
import com.webcrawler.backend.domain.CrawlJob;
import com.webcrawler.backend.web.dto.CrawlRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrawlerService {

    CrawlJob startCrawl(CrawlRequest request, AppUser owner);

    List<CrawlJob> listJobs(AppUser owner);

    Optional<CrawlJob> getJob(UUID jobId, AppUser owner);
}
