package com.webcrawler.backend;

import com.webcrawler.backend.config.AppProperties;
import com.webcrawler.backend.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({JwtProperties.class, AppProperties.class})
public class WebcrawlerBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebcrawlerBackendApplication.class, args);
	}

}
