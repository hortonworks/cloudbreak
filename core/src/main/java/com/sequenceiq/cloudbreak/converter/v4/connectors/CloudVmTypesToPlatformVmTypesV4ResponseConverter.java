package com.sequenceiq.cloudbreak.converter.v4.connectors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformVmtypesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.VirtualMachinesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.VmTypeV4Response;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class CloudVmTypesToPlatformVmTypesV4ResponseConverter
        extends AbstractConversionServiceAwareConverter<CloudVmTypes, PlatformVmtypesV4Response> {

    @Override
    public PlatformVmtypesV4Response convert(CloudVmTypes source) {
        Map<String, VirtualMachinesV4Response> result = new HashMap<>();
        for (Entry<String, Set<VmType>> entry : source.getCloudVmResponses().entrySet()) {
            Set<VmTypeV4Response> vmTypeV4Responses = new HashSet<>();
            for (VmType vmType : entry.getValue()) {
                vmTypeV4Responses.add(getConversionService().convert(vmType, VmTypeV4Response.class));
            }
            VmTypeV4Response defaultVmType = getConversionService().convert(source.getDefaultCloudVmResponses().get(entry.getKey()), VmTypeV4Response.class);

            VirtualMachinesV4Response virtualMachinesV4Response = new VirtualMachinesV4Response();
            virtualMachinesV4Response.setDefaultVirtualMachine(defaultVmType);
            virtualMachinesV4Response.setVirtualMachines(vmTypeV4Responses);
            result.put(entry.getKey(), virtualMachinesV4Response);
        }
        return new PlatformVmtypesV4Response(result);
    }
}
