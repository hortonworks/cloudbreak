package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.platform.GetVirtualMachineTypesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetVirtualMachineTypesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVirtualMachines;

import reactor.bus.Event;

@Component
public class GetVirtualMachineTypesHandler implements CloudPlatformEventHandler<GetVirtualMachineTypesRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetVirtualMachineTypesHandler.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Override
    public Class<GetVirtualMachineTypesRequest> type() {
        return GetVirtualMachineTypesRequest.class;
    }

    @Override
    public void accept(Event<GetVirtualMachineTypesRequest> getVirtualMachineTypesRequestEvent) {
        LOGGER.info("Received event: {}", getVirtualMachineTypesRequestEvent);
        GetVirtualMachineTypesRequest request = getVirtualMachineTypesRequestEvent.getData();
        try {
            PlatformVirtualMachines pv = new PlatformVirtualMachines();
            for (Map.Entry<String, Collection<String>> connector : cloudPlatformConnectors.getPlatformVariants().getPlatformToVariants().entrySet()) {
                String virtualMachine = cloudPlatformConnectors.getDefault(connector.getKey()).parameters().defaultVirtualMachine();
                Map<String, String> stringStringMap = cloudPlatformConnectors.getDefault(connector.getKey()).parameters().virtualMachines();
                pv.getDefaultVirtualMachines().put(connector.getKey(), virtualMachine);
                pv.getVirtualMachines().put(connector.getKey(), stringStringMap);
            }
            GetVirtualMachineTypesResult getVirtualMachineTypesResult = new GetVirtualMachineTypesResult(request, pv);
            request.getResult().onNext(getVirtualMachineTypesResult);
            LOGGER.info("Query platform machine types types finished.");
        } catch (Exception e) {
            request.getResult().onNext(new GetVirtualMachineTypesResult(e.getMessage(), e, request));
        }
    }
}
