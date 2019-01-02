package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.cumulus;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.CredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.providers.CloudPlatform;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CumulusYarnCredentialV4Parameters implements CredentialV4Parameters {

    @NotNull
    @ApiModelProperty(required = true)
    private String ambariPassword;

    @NotNull
    @ApiModelProperty(required = true)
    private String ambariUrl;

    @NotNull
    @ApiModelProperty(required = true)
    private String ambariUser;

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.CUMULUS_YARN;
    }

    public String getAmbariPassword() {
        return ambariPassword;
    }

    public void setAmbariPassword(String ambariPassword) {
        this.ambariPassword = ambariPassword;
    }

    public String getAmbariUrl() {
        return ambariUrl;
    }

    public void setAmbariUrl(String ambariUrl) {
        this.ambariUrl = ambariUrl;
    }

    public String getAmbariUser() {
        return ambariUser;
    }

    public void setAmbariUser(String ambariUser) {
        this.ambariUser = ambariUser;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("cumulusAmbariPassword", ambariPassword);
        map.put("cumulusAmbariUrl", ambariUrl);
        map.put("cumulusAmbariUser", ambariUser);
        return map;
    }

}
