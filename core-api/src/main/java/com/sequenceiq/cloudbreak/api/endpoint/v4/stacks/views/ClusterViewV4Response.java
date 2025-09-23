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
import com.sequenceiq.common.api.type.CertExpirationState;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(Include.NON_NULL)
public class ClusterViewV4Response extends CompactViewV4Response {
    @Schema(description = ClusterModelDescription.STATUS)
    private Status status;

    @Schema(description = ClusterModelDescription.HOSTGROUPS, requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<HostGroupViewV4Response> hostGroups = new HashSet<>();

    @Deprecated
    @Schema(description = ClusterModelDescription.SHARED_SERVICE)
    private SharedServiceV4Response sharedServiceResponse;

    @Schema(description = ClusterModelDescription.BLUEPRINT)
    private BlueprintV4ViewResponse blueprint;

    @Schema(description = ModelDescriptions.StackModelDescription.SERVER_IP)
    private String serverIp;

    @Schema(description = ClusterModelDescription.CERT_EXPIRATION)
    private CertExpirationState certExpirationState;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
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

    public CertExpirationState getCertExpirationState() {
        return certExpirationState;
    }

    public void setCertExpirationState(CertExpirationState certExpirationState) {
        this.certExpirationState = certExpirationState;
    }

    @Override
    public String toString() {
        return "ClusterViewV4Response{ " +
                super.toString() +
                " status=" + status +
                ", hostGroups=" + hostGroups +
                ", sharedServiceResponse=" + sharedServiceResponse +
                ", blueprint=" + blueprint +
                ", serverIp='" + serverIp + '\'' +
                ", certExpirationState=" + certExpirationState +
                '}';
    }
}
