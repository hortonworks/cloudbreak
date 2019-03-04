package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetTlsInfoRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.GetTlsInfoResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class GetTlsInfoHandler implements CloudPlatformEventHandler<GetTlsInfoRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetTlsInfoHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EventBus eventBus;

    @Override
    public Class<GetTlsInfoRequest> type() {
        return GetTlsInfoRequest.class;
    }

    @Override
    public void accept(Event<GetTlsInfoRequest> getTlsInfoRequestEvent) {
        LOGGER.debug("Received event: {}", getTlsInfoRequestEvent);
        GetTlsInfoRequest<?> tlsInfoRequest = getTlsInfoRequestEvent.getData();
        try {
            CloudContext cloudContext = tlsInfoRequest.getCloudContext();
            CloudCredential cloudCredential = tlsInfoRequest.getCloudCredential();
            CloudConnector<Object> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            CloudStack cloudStack = tlsInfoRequest.getCloudStack();
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, cloudCredential);
            TlsInfo tlsInfo = connector.resources().getTlsInfo(ac, cloudStack);
            GetTlsInfoResult getTlsInfoResult = new GetTlsInfoResult(tlsInfoRequest, tlsInfo);
            tlsInfoRequest.getResult().onNext(getTlsInfoResult);
            eventBus.notify(getTlsInfoResult.selector(), new Event<>(getTlsInfoRequestEvent.getHeaders(), getTlsInfoResult));
            LOGGER.debug("GetTlsInfoHandler finished.");
        } catch (RuntimeException e) {
            String errorMsg = "Failed to get TlS info from cloud connector!";
            LOGGER.warn(errorMsg, e);
            GetTlsInfoResult failure = new GetTlsInfoResult(errorMsg, e, tlsInfoRequest);
            tlsInfoRequest.getResult().onNext(failure);
            eventBus.notify(failure.selector(), new Event<>(getTlsInfoRequestEvent.getHeaders(), failure));
        }
    }
}
