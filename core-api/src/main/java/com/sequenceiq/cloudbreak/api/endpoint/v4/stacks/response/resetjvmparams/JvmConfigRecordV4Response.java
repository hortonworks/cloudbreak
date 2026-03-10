package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.resetjvmparams;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JvmConfigRecordV4Response {

    @Schema(description = "Config parameter name")
    private String name;

    @Schema(description = "Config parameter value")
    private String value;

    @Schema(description = "Name of the role config group that owns this config")
    private String roleConfigGroupName;

    @Schema(description = "Name of the cluster the owning service belongs to")
    private String clusterName;

    @Schema(description = "Name of the service that owns this config")
    private String serviceName;

    @Schema(description = "Indicates whether this config will be changed by the recalculation")
    private String applicability;

    public JvmConfigRecordV4Response() {
    }

    public JvmConfigRecordV4Response(String name, String value, String roleConfigGroupName,
            String clusterName, String serviceName, String applicability) {
        this.name = name;
        this.value = value;
        this.roleConfigGroupName = roleConfigGroupName;
        this.clusterName = clusterName;
        this.serviceName = serviceName;
        this.applicability = applicability;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getRoleConfigGroupName() {
        return roleConfigGroupName;
    }

    public void setRoleConfigGroupName(String roleConfigGroupName) {
        this.roleConfigGroupName = roleConfigGroupName;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getApplicability() {
        return applicability;
    }

    public void setApplicability(String applicability) {
        this.applicability = applicability;
    }

    @Override
    public String toString() {
        return "JvmConfigRecordV4Response{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", roleConfigGroupName='" + roleConfigGroupName + '\'' +
                ", clusterName='" + clusterName + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", applicability='" + applicability + '\'' +
                '}';
    }
}
