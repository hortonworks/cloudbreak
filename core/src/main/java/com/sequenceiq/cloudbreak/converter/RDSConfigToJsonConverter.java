package com.sequenceiq.cloudbreak.converter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.RDSConfigResponse;
import com.sequenceiq.cloudbreak.api.model.RdsConfigPropertyJson;
import com.sequenceiq.cloudbreak.common.type.RdsType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;

@Component
public class RDSConfigToJsonConverter extends AbstractConversionServiceAwareConverter<RDSConfig, RDSConfigResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RDSConfigToJsonConverter.class);

    @Override
    public RDSConfigResponse convert(RDSConfig source) {
        RDSConfigResponse json = new RDSConfigResponse();
        json.setName(source.getName());
        json.setConnectionURL(source.getConnectionURL());
        json.setConnectionUserName(source.getConnectionUserName());
        json.setConnectionPassword(source.getConnectionPassword());
        json.setDatabaseType(source.getDatabaseType());
        json.setId(source.getId().toString());
        json.setPublicInAccount(source.isPublicInAccount());
        json.setCreationDate(source.getCreationDate());
        json.setClusterNames(source.getClusters().stream().map(cluster -> cluster.getName()).collect(Collectors.toSet()));
        json.setHdpVersion(source.getHdpVersion());
        json.setType(source.getType() == null ? RdsType.HIVE : source.getType());
        if (source.getAttributes() != null) {
            json.setProperties(convertRdsConfigs(source.getAttributes()));
        }
        return json;
    }

    private Set<RdsConfigPropertyJson> convertRdsConfigs(Json inputs) {
        Set<RdsConfigPropertyJson> rdsConfigPropertyJsons = new HashSet<>();
        try {
            if (inputs.getValue() != null) {
                Map<String, String> is = inputs.get(Map.class);
                for (Map.Entry<String, String> stringStringEntry : is.entrySet()) {
                    RdsConfigPropertyJson rdsConfigPropertyJson = new RdsConfigPropertyJson();
                    rdsConfigPropertyJson.setName(stringStringEntry.getKey());
                    rdsConfigPropertyJson.setValue(stringStringEntry.getValue());
                    rdsConfigPropertyJsons.add(rdsConfigPropertyJson);
                }
            }
        } catch (IOException ex) {
            LOGGER.error("Could not convert rdsConfigPropertyJsons json to Set.");
        }
        return rdsConfigPropertyJsons;

    }
}
