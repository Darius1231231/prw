package com.webcrawler.backend.repository;

import com.webcrawler.backend.domain.CrawlTask;
import com.webcrawler.backend.domain.TaskStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CrawlTaskRepository extends JpaRepository<CrawlTask, UUID> {
    boolean existsByJobIdAndUrl(UUID jobId, String url);
    
    List<CrawlTask> findByAssignedNodeIdAndStatus(UUID assignedNodeId, TaskStatus status);
    
    List<CrawlTask> findByStatus(TaskStatus status, Pageable pageable);

    @Query("SELECT count(t) FROM CrawlTask t WHERE t.job.id = :jobId AND t.status = 'COMPLETED'")
    long countCompletedByJobId(UUID jobId);
    
    @Query("SELECT count(t) FROM CrawlTask t WHERE t.job.id = :jobId")
    long countTotalByJobId(UUID jobId);
}
