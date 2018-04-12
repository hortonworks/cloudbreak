package com.sequenceiq.cloudbreak.converter;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.sequenceiq.cloudbreak.api.model.PlatformVirtualMachinesJson;
import com.sequenceiq.cloudbreak.api.model.VmTypeJson;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVirtualMachines;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;
import com.sequenceiq.cloudbreak.converter.util.PlatformConverterUtil;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

@Component
public class PlatformVirtualMachineTypesToPlatformVirtualMachinesJsonConverter extends AbstractConversionServiceAwareConverter<PlatformVirtualMachines,
        PlatformVirtualMachinesJson> {

    @Override
    public PlatformVirtualMachinesJson convert(PlatformVirtualMachines source) {
        PlatformVirtualMachinesJson json = new PlatformVirtualMachinesJson();
        json.setDefaultVirtualMachines(PlatformConverterUtil.convertDefaults(source.getDefaultVirtualMachines()));
        json.setVirtualMachines(convertVmMap(source.getVirtualMachines()));
        json.setVmTypesPerZones(convertVmsPerZoneMap(source.getVmTypesPerZones()));
        json.setDefaultVmTypePerZones(convertDefaultVmsPerZoneMap(source.getDefaultVmTypePerZones()));
        return json;
    }

    public Map<String, Map<String, String>> convertDefaultVmsPerZoneMap(Map<Platform, Map<AvailabilityZone, VmType>> defaultVmsPerZone) {
        Map<String, Map<String, String>> result = Maps.newHashMap();
        for (Entry<Platform, Map<AvailabilityZone, VmType>> entry : defaultVmsPerZone.entrySet()) {
            Map<String, String> zoneResult = Maps.newHashMap();
            result.put(entry.getKey().value(), zoneResult);
            for (Entry<AvailabilityZone, VmType> zoneEntry : entry.getValue().entrySet()) {
                zoneResult.put(zoneEntry.getKey().value(), zoneEntry.getValue().value());
            }
        }
        return result;
    }

    public Map<String, Map<String, Collection<VmTypeJson>>> convertVmsPerZoneMap(Map<Platform, Map<AvailabilityZone, Collection<VmType>>> zoneVms) {
        Map<String, Map<String, Collection<VmTypeJson>>> result = Maps.newHashMap();
        for (Entry<Platform, Map<AvailabilityZone, Collection<VmType>>> entry : zoneVms.entrySet()) {
            result.put(entry.getKey().value(), convertVmMap(entry.getValue()));
        }
        return result;
    }

    public Map<String, Collection<VmTypeJson>> convertVmMap(Map<? extends StringType, Collection<VmType>> vms) {
        Map<String, Collection<VmTypeJson>> result = Maps.newHashMap();
        for (Entry<? extends StringType, Collection<VmType>> entry : vms.entrySet()) {
            result.put(entry.getKey().value(), convertVmList(entry.getValue()));
        }
        return result;
    }

    public Collection<VmTypeJson> convertVmList(Iterable<VmType> vmlist) {
        Collection<VmTypeJson> result = Lists.newArrayList();
        for (VmType item : vmlist) {
            result.add(getConversionService().convert(item, VmTypeJson.class));
        }
        return result;
    }
}
