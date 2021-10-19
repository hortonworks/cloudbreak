package com.sequenceiq.cloudbreak.cloud.model.catalog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageStackDetails {

    private static final String VERSION = "version";

    private static final String REPO = "repo";

    private static final String BUILD_NUMBER = "build-number";

    private final String version;

    private final StackRepoDetails repo;

    private final String stackBuildNumber;

    @JsonCreator
    public ImageStackDetails(
            @JsonProperty(value = VERSION, required = true) String version,
            @JsonProperty(value = REPO, required = true) StackRepoDetails repo,
            @JsonProperty(BUILD_NUMBER) String stackBuildNumber) {
        this.version = version;
        this.repo = repo;
        this.stackBuildNumber = stackBuildNumber;
    }

    @JsonProperty(VERSION)
    public String getVersion() {
        return version;
    }

    @JsonProperty(REPO)
    public StackRepoDetails getRepo() {
        return repo;
    }

    @JsonProperty(BUILD_NUMBER)
    public String getStackBuildNumber() {
        return stackBuildNumber;
    }
}
