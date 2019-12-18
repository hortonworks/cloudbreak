package com.sequenceiq.cloudbreak.cloud.model.catalog;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StackDetails {

    private final String version;

    private final StackRepoDetails repo;

    private final List<Mpack> mpackList;

    private final String stackBuildNumber;

    @JsonCreator
    public StackDetails(
            @JsonProperty(value = "version", required = true) String version,
            @JsonProperty(value = "repo", required = true) StackRepoDetails repo,
            @JsonProperty("mpacks") List<Mpack> mpackList,
            @JsonProperty("build-number") String stackBuildNumber) {
        this.version = version;
        this.repo = repo;
        this.mpackList = mpackList == null ? Collections.emptyList() : mpackList;
        this.stackBuildNumber = stackBuildNumber;
    }

    public String getVersion() {
        return version;
    }

    public StackRepoDetails getRepo() {
        return repo;
    }

    public List<Mpack> getMpackList() {
        return mpackList;
    }

    public String getStackBuildNumber() {
        return stackBuildNumber;
    }
}
