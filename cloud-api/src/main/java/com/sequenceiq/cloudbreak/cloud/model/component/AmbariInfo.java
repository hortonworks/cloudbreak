package com.sequenceiq.cloudbreak.cloud.model.component;

import java.util.Map;

public class AmbariInfo {

    private String version;

    private Map<String, AmbariRepoDetails> repo;

    private boolean paywallProtected;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, AmbariRepoDetails> getRepo() {
        return repo;
    }

    public void setRepo(Map<String, AmbariRepoDetails> repo) {
        this.repo = repo;
    }

    public boolean isPaywallProtected() {
        return paywallProtected;
    }

    public void setPaywallProtected(boolean paywallProtected) {
        this.paywallProtected = paywallProtected;
    }
}
