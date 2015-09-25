package com.sequenceiq.cloudbreak.service.stack;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformVariantsRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformVariantsResult;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Service
public class DefaultPlatformVariantService implements PlatformVariantService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStackService.class);

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;
    @Inject
    private EventBus eventBus;

    @Override
    public PlatformVariants getPlatformVariants() {
        LOGGER.debug("Get platform variants");
        GetPlatformVariantsRequest getPlatformVariantsRequest = new GetPlatformVariantsRequest();
        eventBus.notify(getPlatformVariantsRequest.selector(), Event.wrap(getPlatformVariantsRequest));
        try {
            GetPlatformVariantsResult res = getPlatformVariantsRequest.await();
            LOGGER.info("Platform variants result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.error("Failed to get platform variants", res.getErrorDetails());
                throw new OperationException(res.getErrorDetails());
            }
            return res.getPlatformVariants();
        } catch (InterruptedException e) {
            LOGGER.error("Error while getting the platform variants", e);
            throw new OperationException(e);
        }
    }
}
