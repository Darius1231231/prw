package com.webcrawler.backend.web;

import com.webcrawler.backend.domain.CrawlerNode;
import com.webcrawler.backend.repository.CrawlerNodeRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nodes")
public class NodeController {

    private final CrawlerNodeRepository nodeRepository;

    public NodeController(CrawlerNodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    @GetMapping
    public List<CrawlerNode> getNodes() {
        return nodeRepository.findAll();
    }
}
