package com.sequenceiq.cloudbreak.service.stack;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentNetworkV4Request;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.event.platform.CloudNetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.CloudNetworkCreationResult;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import reactor.bus.EventBus;

@Service
public class CloudNetworkCreationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudParameterService.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private CloudNetworkCreationRequestFactory cloudNetworkCreationRequestFactory;

    public CreatedCloudNetwork createCloudNetwork(String envName, Credential credential, String cloudPlatform, String region,
            EnvironmentNetworkV4Request networkRequest) {
        LOGGER.debug("Create networks");
        CloudNetworkCreationRequest cloudNetworkCreationRequest = cloudNetworkCreationRequestFactory.create(envName, credential, cloudPlatform, region,
                networkRequest);

        eventBus.notify(cloudNetworkCreationRequest.selector(), eventFactory.createEvent(cloudNetworkCreationRequest));
        try {
            CloudNetworkCreationResult res = cloudNetworkCreationRequest.await();
            LOGGER.debug("Create network result: {}", res);
            if (res.getStatus().equals(EventStatus.FAILED)) {
                LOGGER.debug("Failed to create networks", res.getErrorDetails());
                throw new GetCloudParameterException(res.getErrorDetails());
            }
            return res.getCreatedCloudNetwork();
        } catch (InterruptedException e) {
            LOGGER.error("Error while creating networks", e);
            throw new OperationException(e);
        }
    }
}
