package com.sequenceiq.cloudbreak.cloud.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AmbariInfo {

    @JsonProperty("cb_versions")
    private List<String> cbVersions;
    @JsonProperty("version")
    private String version;
    @JsonProperty("repo")
    private Map<String, String> repo;
    @JsonProperty("hdp")
    private List<HDPInfo> hdp;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, String> getRepo() {
        return repo;
    }

    public void setRepo(Map<String, String> repo) {
        this.repo = repo;
    }

    public List<HDPInfo> getHdp() {
        return hdp;
    }

    public void setHdp(List<HDPInfo> hdp) {
        this.hdp = hdp;
    }

    public List<String> getCbVersions() {
        return cbVersions;
    }

    public void setCbVersions(List<String> cbVersions) {
        this.cbVersions = cbVersions;
    }
}
