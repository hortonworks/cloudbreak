package com.sequenceiq.cloudbreak.api.model;

import java.util.Map;

public class AmbariInfoJson {

    private String version;

    private Map<String, AmbariRepoDetailsJson> repo;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, AmbariRepoDetailsJson> getRepo() {
        return repo;
    }

    public void setRepo(Map<String, AmbariRepoDetailsJson> repo) {
        this.repo = repo;
    }
}
