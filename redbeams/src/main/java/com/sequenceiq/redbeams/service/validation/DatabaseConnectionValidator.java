package com.sequenceiq.redbeams.service.validation;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import com.sequenceiq.redbeams.domain.DatabaseConfig;

@Component
public class DatabaseConnectionValidator extends BaseConnectionValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return DatabaseConfig.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        DatabaseConfig database = (DatabaseConfig) target;
        validate(database.getConnectorJarUrl(), database.getDatabaseVendor(), database.getConnectionURL(),
            database.getConnectionUserName().getRaw(), database.getConnectionPassword().getRaw(), errors);
    }

}
