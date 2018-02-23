package com.sequenceiq.cloudbreak.converter;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.RdsConfigPropertyJson;
import com.sequenceiq.cloudbreak.api.model.RdsType;
import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;

@Component
public class RDSConfigRequestToRDSConfigConverter extends AbstractConversionServiceAwareConverter<RDSConfigRequest, RDSConfig> {

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Override
    public RDSConfig convert(RDSConfigRequest source) {
        RDSConfig rdsConfig = new RDSConfig();
        if (Strings.isNullOrEmpty(source.getName())) {
            rdsConfig.setName(missingResourceNameGenerator.generateName(APIResourceType.RDS_CONFIG));
        } else {
            rdsConfig.setName(source.getName());
        }
        rdsConfig.setConnectionURL(source.getConnectionURL());
        rdsConfig.setConnectionUserName(source.getConnectionUserName());
        rdsConfig.setConnectionPassword(source.getConnectionPassword());
        rdsConfig.setCreationDate(new Date().getTime());
        rdsConfig.setDatabaseType(source.getDatabaseType());
        rdsConfig.setStatus(ResourceStatus.USER_MANAGED);
        rdsConfig.setHdpVersion(source.getHdpVersion());
        rdsConfig.setType(source.getType() == null ? RdsType.HIVE : source.getType());
        try {
            Json json = source.getProperties() == null ? new Json(new HashMap<>())
                : new Json(convertPropertiesToJson(source.getProperties()));
            rdsConfig.setAttributes(json);
        } catch (JsonProcessingException ignored) {
            rdsConfig.setAttributes(null);
        }
        return rdsConfig;
    }

    private Map<String, String> convertPropertiesToJson(Iterable<RdsConfigPropertyJson> inputs) {
        Map<String, String> attributes = new HashMap<>();
        for (RdsConfigPropertyJson input : inputs) {
            attributes.put(input.getName(), input.getValue());
        }
        return attributes;
    }
}