package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UploadIdentityProviderMetadataRequest extends StackEvent {
    public UploadIdentityProviderMetadataRequest(long stackId) {
        super(stackId);
    }
}
