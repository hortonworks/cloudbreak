package com.sequenceiq.cloudbreak.common.database;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

/**
 * Common database functions.
 */
@Component
public class DatabaseCommon {
    private static final String JDBC_REGEX =
            "^(?:jdbc:(oracle|mysql|mariadb|postgresql)(?::[^:]+)?):(?:@|//)(.*?):(\\d*)[:/]?(\\w[-\\w]*)?(?:[?](?:[^=&]*=[^&=]*&?)*)?";
    //                |_______________________________||__________|         |___| |____|     |__________|       |___________________|
    //                               |                      |                 |     |             |                       |
    //                    database type / driver ID         |                host  port     database name (optional)      |
    //                                     additional driver specifiers (e.g., "thin")             URL query parameters (e.g., SSL options)

    private static final Pattern URL_PATTERN = Pattern.compile(JDBC_REGEX);

    private static final int VENDOR_DRIVER_ID_GROUP = 1;

    private static final int HOST_GROUP = 2;

    private static final int PORT_GROUP = 3;

    private static final int DATABASE_GROUP = 4;

    public boolean isValidJdbcConnectionUrl(String connectionUrl) {
        try {
            parseJdbcConnectionUrl(connectionUrl);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public JdbcConnectionUrlFields parseJdbcConnectionUrl(String connectionUrl) {
        Matcher matcher = URL_PATTERN.matcher(connectionUrl);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid JDBC connection URL: " + connectionUrl);
        }

        String vendorDriverId = matcher.group(VENDOR_DRIVER_ID_GROUP);
        if (Strings.isNullOrEmpty(vendorDriverId)) {
            throw new IllegalArgumentException("Vendor driver ID missing from JDBC connection URL: " + connectionUrl);
        }
        String host = matcher.group(HOST_GROUP);
        if (Strings.isNullOrEmpty(vendorDriverId)) {
            throw new IllegalArgumentException("Host missing from JDBC connection URL: " + connectionUrl);
        }
        String portString = matcher.group(PORT_GROUP);
        if (Strings.isNullOrEmpty(portString)) {
            throw new IllegalArgumentException("Port missing from JDBC connection URL: " + connectionUrl);
        }
        int port = Integer.parseInt(portString);
        Optional<String> database = Optional.ofNullable(matcher.group(DATABASE_GROUP));

        if ("postgresql".equals(vendorDriverId) && !database.isPresent()) {
            throw new IllegalArgumentException("PostgreSQL connection URLs require a database");
        }

        return new JdbcConnectionUrlFields(vendorDriverId, host, port, database);
    }

    public String getJdbcConnectionUrl(String vendorDriverId, String host, int port, Optional<String> database) {
        return getJdbcConnectionUrl(new JdbcConnectionUrlFields(vendorDriverId, host, port, database));
    }

    public String getJdbcConnectionUrl(JdbcConnectionUrlFields fields) {
        String url;
        switch (fields.getVendorDriverId()) {
            case "postgresql":
                url = String.format("jdbc:postgresql://%s:%d/", fields.getHost(), fields.getPort());
                if (fields.getDatabase().isPresent()) {
                    url += fields.getDatabase().get();
                } else {
                    // PostgreSQL requires a database for connecting; this is a suitable default
                    url += "postgres";
                }
                break;
            case "mysql":
                // this includes mariadb
                url = String.format("jdbc:mysql://%s:%d", fields.getHost(), fields.getPort());
                if (fields.getDatabase().isPresent()) {
                    url += "/" + fields.getDatabase().get();
                }
                break;
            case "oracle":
                // using sid format, not service format
                url = String.format("jdbc:oracle:thin:@%s:%d", fields.getHost(), fields.getPort());
                if (fields.getDatabase().isPresent()) {
                    url += ":" + fields.getDatabase().get();
                }
                break;
            default:
                throw new UnsupportedOperationException("Don't know how to form a connection URL for JDBC driver " + fields.getVendorDriverId());
        }
        return url;
    }

    public static class JdbcConnectionUrlFields {
        private static final int LOWEST_VALID_PORT = 0;

        private static final int HIGHEST_VALID_PORT = 65535;

        private final String vendorDriverId;

        private final String host;

        private final int port;

        private final Optional<String> database;

        private final String hostAndPort;

        public JdbcConnectionUrlFields(String vendorDriverId, String host, int port, Optional<String> database) {
            if (port < LOWEST_VALID_PORT || port > HIGHEST_VALID_PORT) {
                throw new IllegalArgumentException("invalid port " + port);
            }

            this.vendorDriverId = requireNonNull(vendorDriverId, "vendorDriverId must not be null");
            this.host = requireNonNull(host, "host must not be null");
            this.port = port;
            this.database = requireNonNull(database, "database must not be null");

            hostAndPort = String.format("%s:%d", host, port);
        }

        public String getVendorDriverId() {
            return vendorDriverId;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public String getHostAndPort() {
            return hostAndPort;
        }

        public Optional<String> getDatabase() {
            return database;
        }
    }
}
