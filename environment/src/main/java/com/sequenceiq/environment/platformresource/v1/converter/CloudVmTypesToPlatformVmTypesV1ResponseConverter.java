package com.sequenceiq.environment.platformresource.v1.converter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformVmtypesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.VirtualMachinesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.VmTypeResponse;

@Component
public class CloudVmTypesToPlatformVmTypesV1ResponseConverter {

    @Inject
    private VmTypeToVmTypeV1ResponseConverter vmTypeToVmTypeV1ResponseConverter;

    public PlatformVmtypesResponse convert(CloudVmTypes source) {
        Map<String, VirtualMachinesResponse> result = new HashMap<>();
        for (Entry<String, Set<VmType>> entry : source.getCloudVmResponses().entrySet()) {
            Set<VmTypeResponse> vmTypeResponse = new HashSet<>();
            for (VmType vmType : entry.getValue()) {
                vmTypeResponse.add(vmTypeToVmTypeV1ResponseConverter.convert(vmType));
            }

            VirtualMachinesResponse virtualMachinesResponse = new VirtualMachinesResponse();
            VmType defaultVmType = source.getDefaultCloudVmResponses().get(entry.getKey());
            if (defaultVmType != null) {
                virtualMachinesResponse.setDefaultVirtualMachine(vmTypeToVmTypeV1ResponseConverter.convert(defaultVmType));
            }
            virtualMachinesResponse.setVirtualMachines(vmTypeResponse);
            result.put(entry.getKey(), virtualMachinesResponse);
        }
        return new PlatformVmtypesResponse(result);
    }
}
