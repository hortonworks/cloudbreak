package com.sequenceiq.redbeams.util;

import java.util.Optional;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseV4Request;

public class DatabaseVendorUtil {

    private DatabaseVendorUtil() {
    }

    public static Optional<DatabaseVendor> getVendorByJdbcUrl(DatabaseV4Request configRequest) {
        for (DatabaseVendor databaseVendor : DatabaseVendor.values()) {
            if (configRequest.getConnectionURL().startsWith(String.format("jdbc:%s:", databaseVendor.jdbcUrlDriverId()))) {
                return Optional.of(databaseVendor);
            }
        }
        return Optional.empty();
    }
}
