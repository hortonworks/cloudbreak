package com.sequenceiq.cloudbreak.cloud.model.component;

import java.util.Map;

public class RepositoryInfo {

    private String version;

    private Map<String, RepositoryDetails> repo;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, RepositoryDetails> getRepo() {
        return repo;
    }

    public void setRepo(Map<String, RepositoryDetails> repo) {
        this.repo = repo;
    }

}
