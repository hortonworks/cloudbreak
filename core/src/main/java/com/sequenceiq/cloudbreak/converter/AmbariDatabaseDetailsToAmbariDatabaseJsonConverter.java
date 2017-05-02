package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;

@Component
public class AmbariDatabaseDetailsToAmbariDatabaseJsonConverter extends AbstractConversionServiceAwareConverter<AmbariDatabase, AmbariDatabaseDetailsJson> {

    @Override
    public AmbariDatabaseDetailsJson convert(AmbariDatabase source) {
        if (source != null) {
            AmbariDatabaseDetailsJson ambariDatabase = new AmbariDatabaseDetailsJson();
            ambariDatabase.setName(source.getName());
            ambariDatabase.setHost(source.getHost());
            ambariDatabase.setPort(source.getPort());
            return ambariDatabase;
        }
        return new AmbariDatabaseDetailsJson();
    }

}
