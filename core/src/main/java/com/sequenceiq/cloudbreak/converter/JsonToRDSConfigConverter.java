package com.sequenceiq.cloudbreak.converter;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.RDSConfigJson;
import com.sequenceiq.cloudbreak.api.model.RdsConfigPropertyJson;
import com.sequenceiq.cloudbreak.common.type.RdsType;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;

@Component
public class JsonToRDSConfigConverter extends AbstractConversionServiceAwareConverter<RDSConfigJson, RDSConfig> {
    @Override
    public RDSConfig convert(RDSConfigJson source) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setName(source.getName());
        rdsConfig.setConnectionURL(source.getConnectionURL());
        rdsConfig.setConnectionUserName(source.getConnectionUserName());
        rdsConfig.setConnectionPassword(source.getConnectionPassword());
        rdsConfig.setCreationDate(new Date().getTime());
        rdsConfig.setDatabaseType(source.getDatabaseType());
        rdsConfig.setStatus(ResourceStatus.USER_MANAGED);
        rdsConfig.setHdpVersion(source.getHdpVersion());
        rdsConfig.setType(source.getType() == null ? RdsType.HIVE : source.getType());
        try {
            Json json = new Json(convertPropertiesToJson(source.getProperties()));
            rdsConfig.setAttributes(source.getProperties() == null ? new Json(new HashMap<>()) : json);
        } catch (JsonProcessingException e) {
            rdsConfig.setAttributes(null);
        }
        return rdsConfig;
    }

    private Map<String, String> convertPropertiesToJson(Set<RdsConfigPropertyJson> inputs) {
        Map<String, String> attributes = new HashMap<>();
        for (RdsConfigPropertyJson input : inputs) {
            attributes.put(input.getName(), input.getValue());
        }
        return attributes;
    }
}