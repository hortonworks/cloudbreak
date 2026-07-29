package com.sequenceiq.environment.platformresource.v1.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.DatabaseVmType;
import com.sequenceiq.environment.api.v1.platformresource.model.DatabaseVmTypeMetaJson;
import com.sequenceiq.environment.api.v1.platformresource.model.DatabaseVmTypeResponse;

@Component
public class DatabaseVmTypeToDatabaseVmTypeResponseConverter {

    public DatabaseVmTypeResponse convert(DatabaseVmType e) {
        DatabaseVmTypeResponse vmTypeResponse = new DatabaseVmTypeResponse();
        vmTypeResponse.setValue(e.value());
        DatabaseVmTypeMetaJson vmTypeMetaJson = new DatabaseVmTypeMetaJson();
        Map<String, Object> properties = new HashMap<>();
        properties.put("AvailabilityZones", Objects.requireNonNullElse(e.getMetaData().getAvailabilityZones(), new ArrayList<>()));
        properties.putAll(e.getMetaData().getProperties() != null ? e.getMetaData().getProperties() : Map.of());
        vmTypeMetaJson.setProperties(properties);
        vmTypeResponse.setDatabaseVmTypeMetaJson(vmTypeMetaJson);
        return vmTypeResponse;
    }
}
