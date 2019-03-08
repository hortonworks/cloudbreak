package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.RepositoryV4Response;

import io.swagger.annotations.ApiModel;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class ClouderaManagerRepositoryV4Response extends RepositoryV4Response {

    public ClouderaManagerRepositoryV4Response withVersion(String version) {
        setVersion(version);
        return this;
    }

    public ClouderaManagerRepositoryV4Response withBaseUrl(String baseUrl) {
        setBaseUrl(baseUrl);
        return this;
    }

    public ClouderaManagerRepositoryV4Response withGpgKeyUrl(String gpgKeyUrl) {
        setGpgKeyUrl(gpgKeyUrl);
        return this;
    }

}
