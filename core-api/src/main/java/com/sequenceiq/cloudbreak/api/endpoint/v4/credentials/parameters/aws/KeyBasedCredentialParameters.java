package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.MappableBase;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class KeyBasedCredentialParameters extends MappableBase {

    @NotNull
    @ApiModelProperty(required = true, example = "ASIBJ34QYCJ1IBLK24KA")
    private String accessKey;

    @NotNull
    @ApiModelProperty(required = true, example = "Ratk5cM9edxGuN6jdGb/8Jf621ZuTVGkoO14GPwN")
    private String secretKey;

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

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("accessKey", accessKey);
        map.put("secretKey", secretKey);
        map.put("selector", AwsSelectorType.KEY_BASED.getName());
        return map;
    }

    @Override
    @JsonIgnore
    @ApiModelProperty(hidden = true)
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        super.parse(parameters);
        accessKey = getParameterOrNull(parameters, "accessKey");
        secretKey = getParameterOrNull(parameters, "secretKey");
    }

}
