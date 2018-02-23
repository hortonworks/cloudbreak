package com.sequenceiq.cloudbreak.api.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.ConnectorModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformVmJson implements JsonEntity {

    @ApiModelProperty(ConnectorModelDescription.VIRTUAL_MACHNES)
    private final Collection<VmTypeJson> virtualMachines = new ArrayList<>();

    @ApiModelProperty(ConnectorModelDescription.DEFAULT_VIRTUAL_MACHINES)
    private final Collection<String> defaultVirtualMachines = new ArrayList<>();

    @ApiModelProperty(ConnectorModelDescription.VIRTUAL_MACHINES_PER_ZONES)
    private final Map<String, Collection<VmTypeJson>> vmTypesPerZones = new HashMap<>();

    @ApiModelProperty(ConnectorModelDescription.DEFAULT_VIRTUAL_MACHINES_PER_ZONES)
    private final Map<String, String> defaultVmTypePerZones = new HashMap<>();

    public Collection<VmTypeJson> getVirtualMachines() {
        return virtualMachines;
    }

    public Collection<String> getDefaultVirtualMachines() {
        return defaultVirtualMachines;
    }

    public Map<String, Collection<VmTypeJson>> getVmTypesPerZones() {
        return vmTypesPerZones;
    }

    public Map<String, String> getDefaultVmTypePerZones() {
        return defaultVmTypePerZones;
    }
}
