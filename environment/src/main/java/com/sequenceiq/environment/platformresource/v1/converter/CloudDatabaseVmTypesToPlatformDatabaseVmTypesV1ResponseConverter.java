package com.sequenceiq.environment.platformresource.v1.converter;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudDatabaseVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.environment.api.v1.platformresource.model.DatabaseVirtualMachinesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDatabaseVmtypesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.VmTypeResponse;

@Component
public class CloudDatabaseVmTypesToPlatformDatabaseVmTypesV1ResponseConverter {

    @Inject
    private VmTypeToVmTypeV1ResponseConverter vmTypeToVmTypeV1ResponseConverter;

    public PlatformDatabaseVmtypesResponse convert(CloudDatabaseVmTypes source) {
        Map<String, DatabaseVirtualMachinesResponse> result = new HashMap<>();
        for (Entry<Region, Set<String>> entry : source.getCloudDatabaseVmResponses().entrySet()) {

            DatabaseVirtualMachinesResponse virtualMachinesResponse = new DatabaseVirtualMachinesResponse();
            virtualMachinesResponse.setVirtualMachines(entry.getValue()
                    .stream()
                    .map(e -> new VmTypeResponse(e))
                    .collect(Collectors.toSet()));

            String defaultVmType = source.getDefaultCloudDatabaseVmResponses().get(entry.getKey());
            virtualMachinesResponse.setDefaultVirtualMachine(new VmTypeResponse(defaultVmType));

            result.put(entry.getKey().value(), virtualMachinesResponse);
        }
        return new PlatformDatabaseVmtypesResponse(result);
    }
}
