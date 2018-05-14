package com.sequenceiq.cloudbreak.validation.externaldatabase;

import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.validation.ValidatorUtil;

public class RdsRequestValidator implements ConstraintValidator<ValidRds, RDSConfigRequest> {

    @Override
    public void initialize(ValidRds constraintAnnotation) {
    }

    @Override
    public boolean isValid(RDSConfigRequest request, ConstraintValidatorContext context) {
        boolean supportedScenario = false;
        Optional<DatabaseVendor> vendorByJdbcUrl = DatabaseVendor.getVendorByJdbcUrl(request);

        Optional<SupportedExternalDatabaseServiceEntry> serviceEntry = SupportedDatabaseProvider.supportedExternalDatabases()
                .stream()
                .filter(item -> item.getName().equals(request.getType()) || item.getDisplayName().equals(request.getType()))
                .findFirst();

        if (serviceEntry.isPresent() && vendorByJdbcUrl.isPresent()) {
            Optional<SupportedDatabaseEntry> databaseEntry = serviceEntry.get().getDatabases()
                    .stream()
                    .filter(item -> item.getDatabaseName().equals(vendorByJdbcUrl.get().name()))
                    .findFirst();
            if (databaseEntry.isPresent()) {
                supportedScenario = true;
            } else {
                String message = String.format("The specified Database and Service combination not supported."
                                + " The supported databases are %s for the %s service.",
                        serviceEntry.get().getDatabases()
                                .stream()
                                .map(SupportedDatabaseEntry::getDatabaseName)
                                .collect(Collectors.toList())
                                .stream()
                                .collect(Collectors.joining(",")), request.getType());
                ValidatorUtil.addConstraintViolation(context, message, "status");
            }
        }

        return supportedScenario;
    }

}