package com.sequenceiq.cloudbreak.cloud.model.component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.cloud.model.Versioned;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StackInfo implements Versioned {

    private String version;

    private DefaultStackRepoDetails repo;

    private String minAmbari;

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMinAmbari() {
        return minAmbari;
    }

    public void setMinAmbari(String minAmbari) {
        this.minAmbari = minAmbari;
    }

    public DefaultStackRepoDetails getRepo() {
        if (repo != null) {
            repo.setHdpVersion(version);
        }
        return repo;
    }

    public void setRepo(DefaultStackRepoDetails repo) {
        this.repo = repo;
    }

    @Override
    public String toString() {
        return "StackInfo{"
                + "version='" + version + '\''
                + ", repo=" + repo
                + ", minAmbari=" + minAmbari
                + '}';
    }
}
