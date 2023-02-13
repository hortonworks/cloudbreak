package com.sequenceiq.it.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integrationtest")
public class ITProps {

    private List<String> suiteFiles;

    public List<String> getSuiteFiles() {
        return suiteFiles;
    }

    public void setSuiteFiles(List<String> suiteFiles) {
        this.suiteFiles = suiteFiles;
    }
}
