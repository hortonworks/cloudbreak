package com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm.repository;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.repository.RepositoryV1Request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClouderaManagerRepositoryV1Request extends RepositoryV1Request {

    public ClouderaManagerRepositoryV1Request withVersion(String version) {
        setVersion(version);
        return this;
    }

    public ClouderaManagerRepositoryV1Request withBaseUrl(String baseUrl) {
        setBaseUrl(baseUrl);
        return this;
    }

    public ClouderaManagerRepositoryV1Request withGpgKeyUrl(String gpgKeyUrl) {
        setGpgKeyUrl(gpgKeyUrl);
        return this;
    }
}
