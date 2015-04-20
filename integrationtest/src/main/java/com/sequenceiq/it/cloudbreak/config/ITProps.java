package com.sequenceiq.it.cloudbreak.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integrationtest")
public class ITProps {
    private Map<String, String> credentialNames;
    private Map<String, List<String>> testSuites;
    private List<String> testTypes;
    private List<String> suiteFiles;

    public void setCredentialNames(Map<String, String> credentialNames) {
        this.credentialNames = credentialNames;
    }

    public Map<String, String> getCredentialNames() {
        return credentialNames;
    }

    public String getCredentialName(String cloudProvider) {
        return credentialNames.get(cloudProvider);
    }

    public void setTestSuites(Map<String, List<String>> testSuites) {
        this.testSuites = testSuites;
    }

    public Map<String, List<String>> getTestSuites() {
        return testSuites;
    }

    public List<String> getTestSuites(String suitesKey) {
        return testSuites.get(suitesKey);
    }

    public List<String> getTestTypes() {
        return testTypes;
    }

    public void setTestTypes(List<String> testTypes) {
        this.testTypes = testTypes;
    }

    public List<String> getSuiteFiles() {
        return suiteFiles;
    }

    public void setSuiteFiles(List<String> suiteFiles) {
        this.suiteFiles = suiteFiles;
    }
}
