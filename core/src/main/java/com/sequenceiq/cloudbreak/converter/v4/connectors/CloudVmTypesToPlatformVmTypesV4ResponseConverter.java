package com.sequenceiq.cloudbreak.converter.v4.connectors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformVmtypesV4Response;
import com.sequenceiq.cloudbreak.api.model.VirtualMachinesResponse;
import com.sequenceiq.cloudbreak.api.model.VmTypeJson;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class CloudVmTypesToPlatformVmTypesV4ResponseConverter
        extends AbstractConversionServiceAwareConverter<CloudVmTypes, PlatformVmtypesV4Response> {

    @Override
    public PlatformVmtypesV4Response convert(CloudVmTypes source) {
        Map<String, VirtualMachinesResponse> result = new HashMap<>();
        for (Entry<String, Set<VmType>> entry : source.getCloudVmResponses().entrySet()) {
            Set<VmTypeJson> vmTypeJsons = new HashSet<>();
            for (VmType vmType : entry.getValue()) {
                vmTypeJsons.add(getConversionService().convert(vmType, VmTypeJson.class));
            }
            VmTypeJson defaultVmType = getConversionService().convert(source.getDefaultCloudVmResponses().get(entry.getKey()), VmTypeJson.class);

            VirtualMachinesResponse virtualMachinesResponse = new VirtualMachinesResponse();
            virtualMachinesResponse.setDefaultVirtualMachine(defaultVmType);
            virtualMachinesResponse.setVirtualMachines(vmTypeJsons);
            result.put(entry.getKey(), virtualMachinesResponse);
        }
        return new PlatformVmtypesV4Response(result);
    }
}
