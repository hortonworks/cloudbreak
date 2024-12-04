package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerRepositoryV4Response;

import io.swagger.v3.oas.annotations.media.Schema;

public class ClouderaManagerInfoV4Response {

    private String version;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, ClouderaManagerRepositoryV4Response> repository = new HashMap<>();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, ClouderaManagerRepositoryV4Response> getRepository() {
        return repository;
    }

    public void setRepository(Map<String, ClouderaManagerRepositoryV4Response> repository) {
        this.repository = repository;
    }
}
