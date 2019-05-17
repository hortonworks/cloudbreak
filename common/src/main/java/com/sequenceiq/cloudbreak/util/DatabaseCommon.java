package com.sequenceiq.cloudbreak.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

/**
 * Common database functions.
 */
public class DatabaseCommon {
    public static final String JDBC_REGEX =
            "^(?:jdbc:(?:oracle|mysql|postgresql)(:(?:.*))?):(@|//)(?:.*?):(?:\\d*)[:/](?:\\w+)(?:-*\\w*)*(?:[?](?:[^=&]*=[^&=]*&?)*)?";

    private static final Pattern HOST_PORT_NAME = Pattern.compile("^(.*?):(\\d*)[:/]?(\\w+)?");

    private static final Pattern DATABASE_TYPE = Pattern.compile("jdbc:(oracle|mysql|postgresql).*");

    private static final int HOST_GROUP_INDEX = 1;

    private static final int HOST_PORT_INDEX = 2;

    private static final int DATABASE_GROUP_INDEX = 3;

    private DatabaseCommon() {
        // Do nothing
    }

    public static Optional<HostAndPortAndDatabaseName> getHostPortAndDatabaseName(String connectionURL) {
        String splitter;
        splitter = connectionURL.indexOf("//") > 0 ? "//" : "@";
        String[] split = connectionURL.split(splitter);

        String withoutJDBCPrefix = split[split.length - 1];

        Matcher matcher = HOST_PORT_NAME.matcher(withoutJDBCPrefix);

        if (matcher.find() && matcher.groupCount() == DATABASE_GROUP_INDEX) {
            String host = matcher.group(HOST_GROUP_INDEX);
            String port = matcher.group(HOST_PORT_INDEX);
            String databaseName = matcher.group(DATABASE_GROUP_INDEX);

            if (!StringUtils.isEmpty(host) && !StringUtils.isEmpty(port) && !StringUtils.isEmpty(databaseName)) {
                return Optional.of(new HostAndPortAndDatabaseName(host, Integer.parseInt(port), databaseName));
            }
        }

        return Optional.empty();
    }

    public static Optional<String> getDatabaseType(String connectionURL) {
        Matcher matcher = DATABASE_TYPE.matcher(connectionURL);
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    public static String getConnectionURL(String vendorDriverId, String host, int port, Optional<String> database) {
        String url;
        switch (vendorDriverId) {
            case "postgresql":
                url = String.format("jdbc:postgresql://%s:%d/", host, port);
                if (database.isPresent()) {
                    url += database.get();
                }
                break;
            case "mysql":
                // this includes mariadb
                url = String.format("jdbc:mysql://%s:%d", host, port);
                if (database.isPresent()) {
                    url += "/" + database.get();
                }
                break;
            case "oracle":
                // using sid format, not service format
                url = String.format("jdbc:oracle:thin:@%s:%d", host, port);
                if (database.isPresent()) {
                    url += ":" + database.get();
                }
                break;
            default:
                throw new UnsupportedOperationException("Don't know how to form a connection URL for JDBC driver " + vendorDriverId);
        }
        return url;
    }
}
