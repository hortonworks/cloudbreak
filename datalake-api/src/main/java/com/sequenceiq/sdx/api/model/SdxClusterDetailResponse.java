package com.sequenceiq.sdx.api.model;

import java.util.Optional;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.common.api.tag.response.TaggedResponse;

public class SdxClusterDetailResponse extends SdxClusterResponse implements TaggedResponse {

    private StackV4Response stackV4Response;

    public SdxClusterDetailResponse() {
    }

    public SdxClusterDetailResponse(SdxClusterResponse sdxClusterResponse, StackV4Response stackV4Response) {
        super(sdxClusterResponse.getCrn(), sdxClusterResponse.getName(), sdxClusterResponse.getStatus(),
                sdxClusterResponse.getStatusReason(), sdxClusterResponse.getEnvironmentName(),
                sdxClusterResponse.getEnvironmentCrn(), sdxClusterResponse.getStackCrn(),
                sdxClusterResponse.getClusterShape(), sdxClusterResponse.getCloudStorageBaseLocation(),
                sdxClusterResponse.getCloudStorageFileSystemType(), sdxClusterResponse.getRuntime(),
                sdxClusterResponse.getRangerRazEnabled(), sdxClusterResponse.getTags(), sdxClusterResponse.getCertExpirationState(),
                sdxClusterResponse.getSdxClusterServiceVersion(), sdxClusterResponse.isCMHAEnabled());
        this.stackV4Response = stackV4Response;
    }

    public StackV4Response getStackV4Response() {
        return stackV4Response;
    }

    public void setStackV4Response(StackV4Response stackV4Response) {
        this.stackV4Response = stackV4Response;
    }

    @Override
    public String getTagValue(String key) {
        return Optional.ofNullable(stackV4Response)
                .map(stack -> stack.getTags())
                .map(tags -> tags.getTagValue(key))
                .orElse(null);
    }

    @Override
    public String toString() {
        return "SdxClusterDetailResponse{ " +
                super.toString() +
                " stackV4Response=" + stackV4Response +
                '}';
    }
}
