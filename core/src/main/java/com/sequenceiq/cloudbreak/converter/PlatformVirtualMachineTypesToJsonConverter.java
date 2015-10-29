package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.PlatformVirtualMachines;
import com.sequenceiq.cloudbreak.controller.json.PlatformVirtualMachinesJson;
import com.sequenceiq.cloudbreak.converter.util.PlatformConverterUtil;

@Component
public class PlatformVirtualMachineTypesToJsonConverter extends AbstractConversionServiceAwareConverter<PlatformVirtualMachines,
        PlatformVirtualMachinesJson> {

    @Override
    public PlatformVirtualMachinesJson convert(PlatformVirtualMachines source) {
        PlatformVirtualMachinesJson json = new PlatformVirtualMachinesJson();
        json.setDefaultVirtualMachines(PlatformConverterUtil.convertDefaults(source.getDefaultVirtualMachines()));
        json.setVirtualMachines(PlatformConverterUtil.convertPlatformMap(source.getVirtualMachines()));
        return json;
    }


}
