package com.sequenceiq.cloudbreak.cloud.model.component;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.cloud.model.Versioned;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StackInfo implements Versioned {

    private String version;

    private StackRepoDetails repo;

    private Map<String, Map<String, String>> images;

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public StackRepoDetails getRepo() {
        if (repo != null) {
            repo.setHdpVersion(version);
        }
        return repo;
    }

    public void setRepo(StackRepoDetails repo) {
        this.repo = repo;
    }

    public Map<String, Map<String, String>> getImages() {
        return images;
    }

    public void setImages(Map<String, Map<String, String>> images) {
        this.images = images;
    }

    @Override
    public String toString() {
        return "StackInfo{"
                + "version='" + version + '\''
                + ", repo=" + repo
                + ", images=" + images
                + '}';
    }
}
