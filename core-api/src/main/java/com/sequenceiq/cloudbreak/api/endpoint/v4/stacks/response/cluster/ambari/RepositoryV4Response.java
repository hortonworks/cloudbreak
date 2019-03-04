package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterManagerRepositoryDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class RepositoryV4Response implements JsonEntity {

    @NotNull
    @ApiModelProperty(value = ClusterManagerRepositoryDescription.VERSION, required = true)
    private String version;

    @ApiModelProperty(ClusterManagerRepositoryDescription.BASE_URL)
    private String baseUrl;

    @ApiModelProperty(ClusterManagerRepositoryDescription.REPO_GPG_KEY)
    private String gpgKeyUrl;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getGpgKeyUrl() {
        return gpgKeyUrl;
    }

    public void setGpgKeyUrl(String gpgKeyUrl) {
        this.gpgKeyUrl = gpgKeyUrl;
    }
}
