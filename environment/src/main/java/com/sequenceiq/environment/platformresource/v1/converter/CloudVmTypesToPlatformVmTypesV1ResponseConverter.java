package com.sequenceiq.environment.platformresource.v1.converter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformVmtypesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.VirtualMachinesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.VmTypeResponse;

@Component
public class CloudVmTypesToPlatformVmTypesV1ResponseConverter
        extends AbstractConversionServiceAwareConverter<CloudVmTypes, PlatformVmtypesResponse> {

    @Override
    public PlatformVmtypesResponse convert(CloudVmTypes source) {
        Map<String, VirtualMachinesResponse> result = new HashMap<>();
        for (Entry<String, Set<VmType>> entry : source.getCloudVmResponses().entrySet()) {
            Set<VmTypeResponse> vmTypeRespons = new HashSet<>();
            for (VmType vmType : entry.getValue()) {
                vmTypeRespons.add(getConversionService().convert(vmType, VmTypeResponse.class));
            }
            VmTypeResponse defaultVmType = getConversionService().convert(source.getDefaultCloudVmResponses().get(entry.getKey()), VmTypeResponse.class);

            VirtualMachinesResponse virtualMachinesResponse = new VirtualMachinesResponse();
            virtualMachinesResponse.setDefaultVirtualMachine(defaultVmType);
            virtualMachinesResponse.setVirtualMachines(vmTypeRespons);
            result.put(entry.getKey(), virtualMachinesResponse);
        }
        return new PlatformVmtypesResponse(result);
    }
}
