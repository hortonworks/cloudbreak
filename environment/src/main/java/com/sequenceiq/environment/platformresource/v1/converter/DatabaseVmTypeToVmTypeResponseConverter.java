package com.sequenceiq.environment.platformresource.v1.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.DatabaseVmType;
import com.sequenceiq.environment.api.v1.platformresource.model.VmTypeMetaJson;
import com.sequenceiq.environment.api.v1.platformresource.model.VmTypeResponse;

@Component
public class DatabaseVmTypeToVmTypeResponseConverter {

    public VmTypeResponse convert(DatabaseVmType e) {
        VmTypeResponse vmTypeResponse = new VmTypeResponse();
        vmTypeResponse.setValue(e.value());
        VmTypeMetaJson vmTypeMetaJson = new VmTypeMetaJson();
        Map<String, Object> properties = new HashMap<>();
        properties.put("AvailabilityZones", Objects.requireNonNullElse(e.getMetaData().getAvailabilityZones(), new ArrayList<>()));
        properties.putAll(e.getMetaData().getProperties() != null ? e.getMetaData().getProperties() : Map.of());
        vmTypeMetaJson.setProperties(properties);
        vmTypeResponse.setVmTypeMetaJson(vmTypeMetaJson);
        return vmTypeResponse;
    }
}
