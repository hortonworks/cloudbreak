package com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.ambarirepository.AmbariRepositoryV4Response;

public class AmbariInfoV4Response {

    private String version;

    private Map<String, AmbariRepositoryV4Response> repository;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, AmbariRepositoryV4Response> getRepository() {
        return repository;
    }

    public void setRepository(Map<String, AmbariRepositoryV4Response> repository) {
        this.repository = repository;
    }
}
