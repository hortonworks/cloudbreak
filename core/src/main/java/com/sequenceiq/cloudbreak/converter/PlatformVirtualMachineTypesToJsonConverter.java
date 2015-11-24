package com.sequenceiq.cloudbreak.converter;

import java.util.Collection;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVirtualMachines;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.controller.json.PlatformVirtualMachinesJson;
import com.sequenceiq.cloudbreak.controller.json.VmTypeJson;
import com.sequenceiq.cloudbreak.converter.util.PlatformConverterUtil;

@Component
public class PlatformVirtualMachineTypesToJsonConverter extends AbstractConversionServiceAwareConverter<PlatformVirtualMachines,
        PlatformVirtualMachinesJson> {

    @Override
    public PlatformVirtualMachinesJson convert(PlatformVirtualMachines source) {
        PlatformVirtualMachinesJson json = new PlatformVirtualMachinesJson();
        json.setDefaultVirtualMachines(PlatformConverterUtil.convertDefaults(source.getDefaultVirtualMachines()));
        json.setVirtualMachines(convertVmMap(source.getVirtualMachines()));
        return json;
    }

    public Map<String, Collection<VmTypeJson>> convertVmMap(Map<Platform, Collection<VmType>> vms) {
        Map<String, Collection<VmTypeJson>> result = Maps.newHashMap();
        for (Map.Entry<Platform, Collection<VmType>> entry : vms.entrySet()) {
            result.put(entry.getKey().value(), convertVmList(entry.getValue()));
        }
        return result;
    }

    public Collection<VmTypeJson> convertVmList(Collection<VmType> vmlist) {
        Collection<VmTypeJson> result = Lists.newArrayList();
        for (VmType item : vmlist) {
            if (item.isMetaSet()) {
                result.add(new VmTypeJson(item.value(),
                        item.getMetaData().maxEphemeralVolumeCount(), item.getMetaData().ephemeralVolumeSize()));
            } else {
                result.add(new VmTypeJson(item.value()));
            }
        }
        return result;
    }
}
