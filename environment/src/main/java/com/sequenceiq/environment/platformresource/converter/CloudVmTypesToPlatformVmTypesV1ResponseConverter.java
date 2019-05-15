package com.sequenceiq.environment.platformresource.converter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.platformresource.model.PlatformVmtypesV1Response;
import com.sequenceiq.environment.api.platformresource.model.VirtualMachinesV1Response;
import com.sequenceiq.environment.api.platformresource.model.VmTypeV1Response;

@Component
public class CloudVmTypesToPlatformVmTypesV1ResponseConverter
        extends AbstractConversionServiceAwareConverter<CloudVmTypes, PlatformVmtypesV1Response> {

    @Override
    public PlatformVmtypesV1Response convert(CloudVmTypes source) {
        Map<String, VirtualMachinesV1Response> result = new HashMap<>();
        for (Entry<String, Set<VmType>> entry : source.getCloudVmResponses().entrySet()) {
            Set<VmTypeV1Response> vmTypeV1Responses = new HashSet<>();
            for (VmType vmType : entry.getValue()) {
                vmTypeV1Responses.add(getConversionService().convert(vmType, VmTypeV1Response.class));
            }
            VmTypeV1Response defaultVmType = getConversionService().convert(source.getDefaultCloudVmResponses().get(entry.getKey()), VmTypeV1Response.class);

            VirtualMachinesV1Response virtualMachinesV1Response = new VirtualMachinesV1Response();
            virtualMachinesV1Response.setDefaultVirtualMachine(defaultVmType);
            virtualMachinesV1Response.setVirtualMachines(vmTypeV1Responses);
            result.put(entry.getKey(), virtualMachinesV1Response);
        }
        return new PlatformVmtypesV1Response(result);
    }
}
