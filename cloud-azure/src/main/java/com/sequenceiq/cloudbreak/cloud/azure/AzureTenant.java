package com.sequenceiq.cloudbreak.cloud.azure;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AzureTenant {

    @JsonProperty(value = "id", access = JsonProperty.Access.WRITE_ONLY)
    private String id;

    /**
     * The tenant ID. For example, 00000000-0000-0000-0000-000000000000.
     */
    @JsonProperty(value = "tenantId", access = JsonProperty.Access.WRITE_ONLY)
    private String tenantId;

    public AzureTenant() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    //BEGIN GENERATED CODE
    @Override
    public String toString() {
        return "AzureTenant{" +
                "id='" + id + '\'' +
                ", tenantId='" + tenantId + '\'' +
                '}';
    }
    //END GENERATED CODE
}
