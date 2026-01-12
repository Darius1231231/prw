package com.webcrawler.backend.repository;

import com.webcrawler.backend.domain.CrawlResult;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlResultRepository extends JpaRepository<CrawlResult, UUID> {
    List<CrawlResult> findByJobIdOrderByCrawledAtAsc(UUID jobId);
}
