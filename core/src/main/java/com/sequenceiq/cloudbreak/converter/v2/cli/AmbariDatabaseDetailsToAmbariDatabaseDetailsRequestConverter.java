package com.sequenceiq.cloudbreak.converter.v2.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class AmbariDatabaseDetailsToAmbariDatabaseDetailsRequestConverter
        extends AbstractConversionServiceAwareConverter<AmbariDatabase, AmbariDatabaseDetailsJson> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariDatabaseDetailsToAmbariDatabaseDetailsRequestConverter.class);

    @Override
    public AmbariDatabaseDetailsJson convert(AmbariDatabase source) {
        if (!source.getVendor().toLowerCase().equals(DatabaseVendor.EMBEDDED.value())) {
            AmbariDatabaseDetailsJson ambariDatabaseDetailsJson = new AmbariDatabaseDetailsJson();
            ambariDatabaseDetailsJson.setHost(source.getHost());
            ambariDatabaseDetailsJson.setName(source.getName());
            ambariDatabaseDetailsJson.setPort(source.getPort());
            ambariDatabaseDetailsJson.setPassword(source.getPassword());
            ambariDatabaseDetailsJson.setUserName(source.getUserName());
            ambariDatabaseDetailsJson.setVendor(DatabaseVendor.fromValue(source.getVendor()));
            return ambariDatabaseDetailsJson;
        }
        return null;
    }

}
