package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4ViewResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.CompactViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.sharedservice.SharedServiceV4Response;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonInclude(Include.NON_NULL)
public class ClusterViewV4Response extends CompactViewV4Response {
    @ApiModelProperty(ClusterModelDescription.STATUS)
    private Status status;

    @ApiModelProperty(ClusterModelDescription.SECURE)
    private boolean secure;

    @ApiModelProperty(ClusterModelDescription.HOSTGROUPS)
    private Set<HostGroupViewV4Response> hostGroups = new HashSet<>();

    @ApiModelProperty(ClusterModelDescription.SHARED_SERVICE)
    private SharedServiceV4Response sharedServiceResponse;

    @ApiModelProperty(ClusterModelDescription.KERBEROSCONFIG_NAME)
    private String kerberosName;

    @ApiModelProperty(ClusterModelDescription.BLUEPRINT)
    private BlueprintV4ViewResponse blueprint;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.SERVER_IP)
    private String serverIp;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public Set<HostGroupViewV4Response> getHostGroups() {
        return hostGroups;
    }

    public void setHostGroups(Set<HostGroupViewV4Response> hostGroups) {
        this.hostGroups = hostGroups;
    }

    public SharedServiceV4Response getSharedServiceResponse() {
        return sharedServiceResponse;
    }

    public void setSharedServiceResponse(SharedServiceV4Response sharedServiceResponse) {
        this.sharedServiceResponse = sharedServiceResponse;
    }

    public String getKerberosName() {
        return kerberosName;
    }

    public void setKerberosName(String kerberosName) {
        this.kerberosName = kerberosName;
    }

    public BlueprintV4ViewResponse getBlueprint() {
        return blueprint;
    }

    public void setBlueprint(BlueprintV4ViewResponse blueprint) {
        this.blueprint = blueprint;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }
}
