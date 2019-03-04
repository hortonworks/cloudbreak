package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.STACK_ID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.environment.EnvironmentSettingsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.UserViewV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonInclude(Include.NON_NULL)
public class StackViewV4Response implements JsonEntity {

    @ApiModelProperty(STACK_ID)
    private Long id;

    @ApiModelProperty(value = StackModelDescription.STACK_NAME, required = true)
    private String name;

    @ApiModelProperty(StackModelDescription.HDP_VERSION)
    private String hdpVersion;

    @ApiModelProperty(StackModelDescription.CLUSTER)
    private ClusterViewV4Response cluster;

    @ApiModelProperty(StackModelDescription.STACK_STATUS)
    private Status status;

    @ApiModelProperty(StackModelDescription.NODE_COUNT)
    private Integer nodeCount;

    @ApiModelProperty(StackModelDescription.CREATED)
    private Long created;

    @ApiModelProperty(StackModelDescription.TERMINATED)
    private Long terminated;

    @ApiModelProperty(StackModelDescription.USER)
    private UserViewV4Response user;

    @ApiModelProperty(StackModelDescription.ENVIRONMENT)
    private EnvironmentSettingsV4Response environment;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ClusterViewV4Response getCluster() {
        return cluster;
    }

    public void setCluster(ClusterViewV4Response cluster) {
        this.cluster = cluster;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getHdpVersion() {
        return hdpVersion;
    }

    public void setHdpVersion(String hdpVersion) {
        this.hdpVersion = hdpVersion;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EnvironmentSettingsV4Response getEnvironment() {
        return environment;
    }

    public void setEnvironment(EnvironmentSettingsV4Response environment) {
        this.environment = environment;
    }

    public UserViewV4Response getUser() {
        return user;
    }

    public void setUser(UserViewV4Response user) {
        this.user = user;
    }

    public Long getTerminated() {
        return terminated;
    }

    public void setTerminated(Long terminated) {
        this.terminated = terminated;
    }

}
