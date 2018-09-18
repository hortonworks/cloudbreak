package com.sequenceiq.cloudbreak.api.model.stack.cluster;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.model.BlueprintViewResponse;
import com.sequenceiq.cloudbreak.api.model.CompactViewResponse;
import com.sequenceiq.cloudbreak.api.model.SharedServiceResponse;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class ClusterViewResponse extends CompactViewResponse {
    @ApiModelProperty(ClusterModelDescription.STATUS)
    private Status status;

    @ApiModelProperty(ClusterModelDescription.SECURE)
    private boolean secure;

    @ApiModelProperty(StackModelDescription.AMBARI_IP)
    private String ambariServerIp;

    @ApiModelProperty(ClusterModelDescription.BLUEPRINT)
    private BlueprintViewResponse blueprint;

    @ApiModelProperty(ClusterModelDescription.HOSTGROUPS)
    private Set<HostGroupViewResponse> hostGroups = new HashSet<>();

    @ApiModelProperty(ClusterModelDescription.SHARED_SERVICE)
    private SharedServiceResponse sharedServiceResponse;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getAmbariServerIp() {
        return ambariServerIp;
    }

    public void setAmbariServerIp(String ambariServerIp) {
        this.ambariServerIp = ambariServerIp;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public Set<HostGroupViewResponse> getHostGroups() {
        return hostGroups;
    }

    public void setHostGroups(Set<HostGroupViewResponse> hostGroups) {
        this.hostGroups = hostGroups;
    }

    public BlueprintViewResponse getBlueprint() {
        return blueprint;
    }

    public void setBlueprint(BlueprintViewResponse blueprint) {
        this.blueprint = blueprint;
    }

    public SharedServiceResponse getSharedServiceResponse() {
        return sharedServiceResponse;
    }

    public void setSharedServiceResponse(SharedServiceResponse sharedServiceResponse) {
        this.sharedServiceResponse = sharedServiceResponse;
    }
}
