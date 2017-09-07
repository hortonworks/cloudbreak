package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetVirtualMachineTypesRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.GetVirtualMachineTypesResult;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVirtualMachines;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypes;

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
            Map<Platform, Map<AvailabilityZone, Collection<VmType>>> platformVmPerZones = Maps.newHashMap();
            Map<Platform, Map<AvailabilityZone, VmType>> platformDefaultVmPerZones = Maps.newHashMap();
            for (Entry<Platform, Collection<Variant>> connector : cloudPlatformConnectors.getPlatformVariants().getPlatformToVariants().entrySet()) {
                Platform platform = connector.getKey();
                PlatformParameters platformParams = cloudPlatformConnectors.getDefault(platform).parameters();
                VmTypes vmTypes =  platformParams.vmTypes(request.getExtended());
                Map<AvailabilityZone, VmTypes> zoneVmTypes = platformParams.vmTypesPerAvailabilityZones(request.getExtended());

                platformDefaultVm.put(platform, vmTypes.defaultType());
                platformVms.put(platform, vmTypes.types());

                Map<AvailabilityZone, Collection<VmType>> vmPerZones = Maps.newHashMap();
                Map<AvailabilityZone, VmType> defaultVmPerZones = Maps.newHashMap();
                for (Entry<AvailabilityZone, VmTypes> types: zoneVmTypes.entrySet()) {
                    vmPerZones.put(types.getKey(), types.getValue().types());
                    defaultVmPerZones.put(types.getKey(), types.getValue().defaultType());
                }
                platformVmPerZones.put(platform, vmPerZones);
                platformDefaultVmPerZones.put(platform, defaultVmPerZones);
            }
            PlatformVirtualMachines pv = new PlatformVirtualMachines(platformVms, platformDefaultVm, platformVmPerZones, platformDefaultVmPerZones);
            GetVirtualMachineTypesResult getVirtualMachineTypesResult = new GetVirtualMachineTypesResult(request, pv);
            request.getResult().onNext(getVirtualMachineTypesResult);
            LOGGER.info("Query platform machine types types finished.");
        } catch (RuntimeException e) {
            request.getResult().onNext(new GetVirtualMachineTypesResult(e.getMessage(), e, request));
        }
    }
}
