package com.sequenceiq.redbeams.service.validation;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.sequenceiq.cloudbreak.validation.DatabaseCommon;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;

@Component
public class DatabaseServerConnectionValidator extends BaseConnectionValidator implements Validator {

    @Inject
    private DatabaseCommon databaseCommon;

    @Override
    public boolean supports(Class<?> clazz) {
        return DatabaseServerConfig.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        DatabaseServerConfig server = (DatabaseServerConfig) target;
        String connectionUrl = databaseCommon.getJdbcConnectionUrl(server.getDatabaseVendor().jdbcUrlDriverId(),
            server.getHost(), server.getPort(), Optional.empty());
        validate(server.getConnectorJarUrl(), server.getDatabaseVendor(), connectionUrl, server.getConnectionUserName(),
            server.getConnectionPassword(), errors);
    }

}
