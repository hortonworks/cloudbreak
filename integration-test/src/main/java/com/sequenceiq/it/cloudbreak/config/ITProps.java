package com.sequenceiq.it.cloudbreak.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integrationtest")
public class ITProps {
    private Map<String, String> credentialNames;

    private Map<String, String> defaultNetworks;

    private Map<String, List<String>> testSuites;

    private List<String> testTypes;

    private List<String> suiteFiles;

    private Map<String, String> defaultSecurityGroups;

    public void setCredentialNames(Map<String, String> credentialNames) {
        this.credentialNames = credentialNames;
    }

    public Map<String, String> getCredentialNames() {
        return credentialNames;
    }

    public String getCredentialName(String cloudProvider) {
        return credentialNames.get(cloudProvider);
    }

    public void setDefaultNetworks(Map<String, String> defaultNetworks) {
        this.defaultNetworks = defaultNetworks;
    }

    public Map<String, String> getDefaultNetworks() {
        return defaultNetworks;
    }

    public String getDefaultNetwork(String cloudProvider) {
        return defaultNetworks.get(cloudProvider);
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

    public Collection<String> getTestTypes() {
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

    public Map<String, String> getDefaultSecurityGroups() {
            return defaultSecurityGroups;
    }

    public String getDefaultSecurityGroup(String cloudProvider) {
        return defaultSecurityGroups.get(cloudProvider);
    }

    public boolean isDefaultSecurityGroup(String securityGroupName) {
        for (String securityGroup : defaultSecurityGroups.values()) {
            if (securityGroup.equals(securityGroupName)) {
                return true;
            }
        }
        return false;
    }

    public void setDefaultSecurityGroups(Map<String, String> defaultSecurityGroups) {
        this.defaultSecurityGroups = defaultSecurityGroups;
    }
}
