package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HDPInfo {

    private String version;
    private HDPRepo repo;
    private Map<String, Map<String, String>> images;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public HDPRepo getRepo() {
        return repo;
    }

    public void setRepo(HDPRepo repo) {
        this.repo = repo;
    }

    public Map<String, Map<String, String>> getImages() {
        return images;
    }

    public void setImages(Map<String, Map<String, String>> images) {
        this.images = images;
    }
}
