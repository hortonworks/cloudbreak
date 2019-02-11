package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.BlueprintV4ViewResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.sharedservice.SharedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.CompactViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ClusterModelDescription;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription;

import io.swagger.annotations.ApiModelProperty;

public class ClusterViewV4Response extends CompactViewV4Response {
    @ApiModelProperty(ClusterModelDescription.STATUS)
    private Status status;

    @ApiModelProperty(ClusterModelDescription.SECURE)
    private boolean secure;

    @ApiModelProperty(StackModelDescription.AMBARI_IP)
    private String ambariServerIp;

    @ApiModelProperty(ClusterModelDescription.BLUEPRINT)
    private BlueprintV4ViewResponse blueprint;

    @ApiModelProperty(ClusterModelDescription.HOSTGROUPS)
    private Set<HostGroupViewV4Response> hostGroups = new HashSet<>();

    @ApiModelProperty(ClusterModelDescription.SHARED_SERVICE)
    private SharedServiceV4Response sharedServiceResponse;

    @ApiModelProperty(ClusterModelDescription.KERBEROSCONFIG_NAME)
    private String kerberosName;

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

    public Set<HostGroupViewV4Response> getHostGroups() {
        return hostGroups;
    }

    public void setHostGroups(Set<HostGroupViewV4Response> hostGroups) {
        this.hostGroups = hostGroups;
    }

    public BlueprintV4ViewResponse getBlueprint() {
        return blueprint;
    }

    public void setBlueprint(BlueprintV4ViewResponse blueprint) {
        this.blueprint = blueprint;
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

}
