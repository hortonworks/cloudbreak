package com.sequenceiq.mock.model;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.mock.config.ConfigParams;

public class MockConfigDto {
    private TestMode testMode;

    private String yarnRecommendationInterval;

    private Map<String, ConfigParams> loadTestConfig = new HashMap<>();

    public TestMode getTestMode() {
        return testMode;
    }

    public void setTestMode(TestMode testMode) {
        this.testMode = testMode;
    }

    public Map<String, ConfigParams> getLoadTestConfig() {
        return loadTestConfig;
    }

    public void setLoadTestConfig(Map<String, ConfigParams> loadTestConfig) {
        this.loadTestConfig = loadTestConfig;
    }

    public String getYarnRecommendationInterval() {
        return yarnRecommendationInterval;
    }

    public void setYarnRecommendationInterval(String yarnRecommendationInterval) {
        this.yarnRecommendationInterval = yarnRecommendationInterval;
    }
}
