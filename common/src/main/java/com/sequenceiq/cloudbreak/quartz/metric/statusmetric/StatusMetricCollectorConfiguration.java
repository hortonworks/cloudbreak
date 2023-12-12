package com.sequenceiq.cloudbreak.quartz.metric.statusmetric;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("status.metrics.collector")
public class StatusMetricCollectorConfiguration {

    private static final int DEFAULT_SCRAPE_INTERVAL_IN_SECONDS = 30;

    private boolean enabled = true;

    private int scrapeIntervalInSeconds = DEFAULT_SCRAPE_INTERVAL_IN_SECONDS;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getScrapeIntervalInSeconds() {
        return scrapeIntervalInSeconds;
    }

    public void setScrapeIntervalInSeconds(int scrapeIntervalInSeconds) {
        this.scrapeIntervalInSeconds = scrapeIntervalInSeconds;
    }
}
