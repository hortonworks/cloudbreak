package com.sequenceiq.redbeams.validation;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.validation.ValidatorUtil;
import com.sequenceiq.cloudbreak.validation.externaldatabase.SupportedDatabaseEntry;
import com.sequenceiq.cloudbreak.validation.externaldatabase.SupportedDatabaseProvider;
import com.sequenceiq.cloudbreak.validation.externaldatabase.SupportedExternalDatabaseServiceEntry;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseV4Request;
import com.sequenceiq.redbeams.api.util.DatabaseVendorUtil;

public class DatabaseVendorAndServiceValidator implements ConstraintValidator<ValidDatabaseVendorAndService, DatabaseV4Request> {

    private DatabaseVendorUtil databaseVendorUtil = new DatabaseVendorUtil();

    @Override
    public boolean isValid(DatabaseV4Request request, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        Optional<DatabaseVendor> vendorByJdbcUrl = databaseVendorUtil.getVendorByJdbcUrl(request.getConnectionURL());
        if (!vendorByJdbcUrl.isPresent()) {
            ValidatorUtil.addConstraintViolation(context, "Could not determine database vendor from JDBC URL "
                + request.getConnectionURL(), "connectionURL");
            return false;
        }

        Optional<SupportedExternalDatabaseServiceEntry> serviceEntry = SupportedDatabaseProvider.supportedExternalDatabases()
                .stream()
                .filter(item -> item.getName().equalsIgnoreCase(request.getType())
                        || item.getDisplayName().equalsIgnoreCase(request.getType()))
                .findFirst();
        if (!serviceEntry.isPresent()) {
            serviceEntry = SupportedDatabaseProvider.getOthers();
        }

        if (serviceEntry.isPresent() && vendorByJdbcUrl.isPresent()) {
            Set<SupportedDatabaseEntry> allSupportedDatabases = serviceEntry.get().getDatabases();
            boolean supported = allSupportedDatabases.stream()
                    .anyMatch(item -> item.getDatabaseName().equals(vendorByJdbcUrl.get().name()));
            if (supported) {
                return true;
            }

            String message = String.format("The specified database vendor for service %s is not supported."
                            + " The supported database vendors are: %s",
                    request.getType(),
                    allSupportedDatabases.stream()
                            .map(SupportedDatabaseEntry::getDatabaseName)
                            .collect(Collectors.joining(", ")));
            ValidatorUtil.addConstraintViolation(context, message, "type");
            return false;
        }

        ValidatorUtil.addConstraintViolation(context, "Could not find database support information for service "
            + request.getType(), "type");
        return false;
    }

}
