package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UploadIdentityProviderMetadataFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UploadIdentityProviderMetadataRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.UploadIdentityProviderMetadataSuccess;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class UploadIdentityProviderMetadataHandler implements EventHandler<UploadIdentityProviderMetadataRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UploadIdentityProviderMetadataHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterServiceRunner clusterServiceRunner;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UploadIdentityProviderMetadataRequest.class);
    }

    @Override
    public void accept(Event<UploadIdentityProviderMetadataRequest> event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterServiceRunner.uploadIdentityProviderMetadata(stackId);
            response = new UploadIdentityProviderMetadataSuccess(stackId);
        } catch (Exception e) {
            LOGGER.info("Upload identity provider metadata failed!", e);
            response = new UploadIdentityProviderMetadataFailed(stackId, e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
