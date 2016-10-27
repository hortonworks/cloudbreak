package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetVirtualMachineTypesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetVirtualMachineTypesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVirtualMachines;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.VmType;

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
            Map<Platform, Collection<VmType>> platformVms = Maps.newHashMap();
            Map<Platform, VmType> platformDefaultVm = Maps.newHashMap();
            for (Map.Entry<Platform, Collection<Variant>> connector : cloudPlatformConnectors.getPlatformVariants().getPlatformToVariants().entrySet()) {
                VmType defaultVm = cloudPlatformConnectors.getDefault(connector.getKey()).parameters().vmTypes(request.getExtended()).defaultType();
                Collection<VmType> vmTypes = cloudPlatformConnectors.getDefault(connector.getKey()).parameters().vmTypes(request.getExtended()).types();

                platformDefaultVm.put(connector.getKey(), defaultVm);
                platformVms.put(connector.getKey(), vmTypes);
            }
            PlatformVirtualMachines pv = new PlatformVirtualMachines(platformVms, platformDefaultVm);
            GetVirtualMachineTypesResult getVirtualMachineTypesResult = new GetVirtualMachineTypesResult(request, pv);
            request.getResult().onNext(getVirtualMachineTypesResult);
            LOGGER.info("Query platform machine types types finished.");
        } catch (Exception e) {
            request.getResult().onNext(new GetVirtualMachineTypesResult(e.getMessage(), e, request));
        }
    }
}
