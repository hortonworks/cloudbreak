package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerRepositoryV4Response;

public class ClouderaManagerInfoV4Response {

    private String version;

    private Map<String, ClouderaManagerRepositoryV4Response> repository;

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
