package com.webcrawler.backend.repository;

import com.webcrawler.backend.domain.CrawlJob;
import com.webcrawler.backend.domain.CrawlStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrawlJobRepository extends JpaRepository<CrawlJob, UUID> {

    @EntityGraph(attributePaths = {"results"})
    Optional<CrawlJob> findWithResultsById(UUID id);

    List<CrawlJob> findByOwnerUsernameOrderByCreatedAtDesc(String username);

    List<CrawlJob> findByStatus(CrawlStatus status);
}
