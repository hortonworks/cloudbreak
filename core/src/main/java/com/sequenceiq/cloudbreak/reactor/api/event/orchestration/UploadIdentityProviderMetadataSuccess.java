package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UploadIdentityProviderMetadataSuccess extends StackEvent {
    public UploadIdentityProviderMetadataSuccess(Long stackId) {
        super(stackId);
    }

    public UploadIdentityProviderMetadataSuccess(String selector, Long stackId) {
        super(selector, stackId);
    }

}
