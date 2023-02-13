package com.sequenceiq.environment.api.v1.credential.model.parameters.openstack;

import static io.swagger.v3.oas.annotations.media.Schema.*;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Deprecated
public class KeystoneV2Parameters implements Serializable {

    @Schema(required = true)
    private String tenantName;

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    @Override
    public String toString() {
        return "KeystoneV2Parameters{" +
                "tenantName='" + tenantName + '\'' +
                '}';
    }
}
