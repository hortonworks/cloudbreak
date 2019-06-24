package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class UploadIdentityProviderMetadataFailed extends StackFailureEvent {
    public UploadIdentityProviderMetadataFailed(Long stackId, Exception ex) {
        super(stackId, ex);
    }
}
