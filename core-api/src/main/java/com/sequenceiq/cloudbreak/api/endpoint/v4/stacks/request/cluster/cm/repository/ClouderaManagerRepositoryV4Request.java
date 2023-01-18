package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.repository.RepositoryV4Request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClouderaManagerRepositoryV4Request extends RepositoryV4Request {

    public ClouderaManagerRepositoryV4Request withVersion(String version) {
        setVersion(version);
        return this;
    }

    public ClouderaManagerRepositoryV4Request withBaseUrl(String baseUrl) {
        setBaseUrl(baseUrl);
        return this;
    }

    public ClouderaManagerRepositoryV4Request withGpgKeyUrl(String gpgKeyUrl) {
        setGpgKeyUrl(gpgKeyUrl);
        return this;
    }

    @Override
    public String toString() {
        return "ClouderaManagerRepositoryV4Request{} " + super.toString();
    }
}
