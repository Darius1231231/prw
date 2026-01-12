package com.webcrawler.backend.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /** Secret key used to sign JWT tokens. */
    private String secret;

    /** Token expiration represented as ISO-8601 duration (e.g. PT24H). */
    private Duration expiration = Duration.ofHours(24);

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Duration getExpiration() {
        return expiration;
    }

    public void setExpiration(Duration expiration) {
        this.expiration = expiration;
    }
}
