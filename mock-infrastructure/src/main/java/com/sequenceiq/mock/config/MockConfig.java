package com.sequenceiq.mock.config;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;

import com.sequenceiq.mock.model.MockConfigDto;
import com.sequenceiq.mock.model.TestMode;

@Configuration
@ConfigurationProperties(prefix = "mock.config")
public class MockConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockConfig.class);

    private TestMode testMode;

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

    @PostConstruct
    public void init() {
        loadTestConfig.entrySet().stream().forEach(entry -> {
            entry.getValue().setUriPattern(entry.getKey());
        });
    }

    public String [] getPathsForIntercept() {
        return  loadTestConfig.keySet().toArray(new String[0]);
    }

    public ConfigParams getConfigParams(String requestUri) {
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        for (Map.Entry<String, ConfigParams> pattern : loadTestConfig.entrySet()) {
            if (antPathMatcher.match(pattern.getKey(), requestUri)) {
                return pattern.getValue();
            }
        }
        return null;
    }

    public MockConfigDto getMockConfigDto() {
        MockConfigDto mockConfigDto = new MockConfigDto();
        mockConfigDto.setTestMode(testMode);
        mockConfigDto.setLoadTestConfig(loadTestConfig);
        return mockConfigDto;
    }

    public void update(MockConfigDto mockConfigDto) {
        this.testMode = mockConfigDto.getTestMode();
        if (mockConfigDto.getLoadTestConfig() != null) {
            mockConfigDto.getLoadTestConfig().entrySet().forEach(entry -> {
                this.loadTestConfig.put(entry.getKey(), entry.getValue());
            });
        }
    }
}
