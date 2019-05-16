package com.sequenceiq.cloudbreak.cloud.model.component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.common.type.Versioned;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DefaultCDHInfo implements Versioned {

    private String version;

    private ClouderaManagerDefaultStackRepoDetails repo;

    private String minCM;

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMinCM() {
        return minCM;
    }

    public void setMinCM(String minCM) {
        this.minCM = minCM;
    }

    public ClouderaManagerDefaultStackRepoDetails getRepo() {
        if (repo != null) {
            repo.setCdhVersion(version);
        }
        return repo;
    }

    public void setRepo(ClouderaManagerDefaultStackRepoDetails repo) {
        this.repo = repo;
    }

    @Override
    public String toString() {
        return "StackInfo{"
                + "version='" + version + '\''
                + ", repo=" + repo
                + ", minCM=" + minCM
                + '}';
    }
}
