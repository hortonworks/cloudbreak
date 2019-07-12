package com.sequenceiq.redbeams.api.util;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;

@Component
public class DatabaseVendorUtil {

    /**
     * Identifies an appropriate database vendor based on a JDBC connection URL.
     * The behavior of the method depends on the ordering of DatabaseVendor
     * enum definitions; a value later in the list that shares the same JDBC URL
     * driver ID with one earlier in the list will <em>never</em> be returned by
     * this method.
     *
     * @param  jdbcUrl JDBC connection URL
     * @return         appropriate database vendor
     */
    public Optional<DatabaseVendor> getVendorByJdbcUrl(String jdbcUrl) {
        for (DatabaseVendor databaseVendor : DatabaseVendor.values()) {
            if (jdbcUrl.startsWith(String.format("jdbc:%s:", databaseVendor.jdbcUrlDriverId()))) {
                return Optional.of(databaseVendor);
            }
        }
        return Optional.empty();
    }
}
