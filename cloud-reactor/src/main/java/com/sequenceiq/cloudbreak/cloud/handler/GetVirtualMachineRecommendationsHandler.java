package com.sequenceiq.cloudbreak.cloud.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetVirtualMachineRecommendationResponse;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetVirtualMachineRecommendtaionRequest;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendations;

import reactor.bus.Event;

@Component
public class GetVirtualMachineRecommendationsHandler implements CloudPlatformEventHandler<GetVirtualMachineRecommendtaionRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetVirtualMachineRecommendationsHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetVirtualMachineRecommendtaionRequest> type() {
        return GetVirtualMachineRecommendtaionRequest.class;
    }

    @Override
    public void accept(Event<GetVirtualMachineRecommendtaionRequest> gVMRRE) {
        LOGGER.debug("Received GetVirtualMachineRecommendtaionRequest event: {}", gVMRRE);
        GetVirtualMachineRecommendtaionRequest request = gVMRRE.getData();
        String cloudPlatform = request.getCloudPlatform();
        try {
            CloudConnector<Object> connector = cloudPlatformConnectors.getDefault(Platform.platform(cloudPlatform));
            VmRecommendations recommendations = connector.parameters().recommendedVms();
            GetVirtualMachineRecommendationResponse gVMRResponse = new GetVirtualMachineRecommendationResponse(request, recommendations);
            request.getResult().onNext(gVMRResponse);
            LOGGER.debug("Query platform machine recommendations finished.");
        } catch (RuntimeException e) {
            LOGGER.error("Could not get virtual machine recommendations for platform: " + cloudPlatform, e);
            request.getResult().onNext(new GetVirtualMachineRecommendationResponse(e.getMessage(), e, request));
        }
    }
}
