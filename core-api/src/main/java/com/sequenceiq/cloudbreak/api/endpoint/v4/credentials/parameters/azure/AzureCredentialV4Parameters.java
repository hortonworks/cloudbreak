package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure;

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
public class AzureCredentialV4Parameters implements CredentialV4Parameters {

    @NotNull
    @ApiModelProperty(required = true)
    private String accessKey;

    @NotNull
    @ApiModelProperty(required = true)
    private String secretKey;

    @NotNull
    @ApiModelProperty(required = true)
    private String subscriptionId;

    @NotNull
    @ApiModelProperty(required = true)
    private String tenantId;

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("accessKey", accessKey);
        map.put("secretKey", secretKey);
        map.put("subscriptionId", subscriptionId);
        map.put("tenantId", tenantId);
        return map;
    }

}
