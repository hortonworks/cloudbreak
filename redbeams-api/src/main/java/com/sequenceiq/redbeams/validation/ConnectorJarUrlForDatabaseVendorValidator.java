package com.sequenceiq.redbeams.validation;

import java.util.EnumSet;
import java.util.Optional;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.validation.ValidatorUtil;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerV4Request;
import com.sequenceiq.redbeams.api.util.DatabaseVendorUtil;

/**
 * Validates whether the connector JAR URL for a request is valid based on its
 * database vendor.
 */
public class ConnectorJarUrlForDatabaseVendorValidator implements ConstraintValidator<ValidConnectorJarUrlForDatabaseVendor, Object> {

    // Connector JAR URL is optional for PostgreSQL only.
    private static final EnumSet<DatabaseVendor> DATABASE_VENDORS_ALLOWING_NULL_CONNECTOR_JAR_URL =
        EnumSet.of(DatabaseVendor.POSTGRES);

    private DatabaseVendorUtil databaseVendorUtil = new DatabaseVendorUtil();

    @Override
    public boolean isValid(Object request, ConstraintValidatorContext context) {
        String connectorJarUrl;
        DatabaseVendor databaseVendor;

        if (request instanceof DatabaseV4Request) {
            DatabaseV4Request req = (DatabaseV4Request) request;
            connectorJarUrl = req.getConnectorJarUrl();
            Optional<DatabaseVendor> vendorByJdbcUrl = databaseVendorUtil.getVendorByJdbcUrl(req.getConnectionURL());
            if (!vendorByJdbcUrl.isPresent()) {
                ValidatorUtil.addConstraintViolation(context, "Could not determine database vendor from JDBC URL "
                    + req.getConnectionURL(), "connectionURL");
                return false;
            }
            databaseVendor = vendorByJdbcUrl.get();
        } else if (request instanceof DatabaseServerV4Request) {
            connectorJarUrl = ((DatabaseServerV4Request) request).getConnectorJarUrl();
            databaseVendor = DatabaseVendor.fromValue(((DatabaseServerV4Request) request).getDatabaseVendor());
        } else if (request instanceof com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4Request) {
            connectorJarUrl = ((com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4Request) request).getConnectorJarUrl();
            databaseVendor = DatabaseVendor.fromValue(((com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4Request) request).getDatabaseVendor());
        } else {
            throw new IllegalStateException("@ValidConnectorJarUrlForDatabaseVendor is applied to request of type "
                + request.getClass().toString() + " which isn't supported");
        }

        if (connectorJarUrl == null && !DATABASE_VENDORS_ALLOWING_NULL_CONNECTOR_JAR_URL.contains(databaseVendor)) {
            ValidatorUtil.addConstraintViolation(context, "Database vendor " + databaseVendor + " requires a connector JAR URL", "connectorJarUrl");
            return false;
        }

        return true;
    }

}
