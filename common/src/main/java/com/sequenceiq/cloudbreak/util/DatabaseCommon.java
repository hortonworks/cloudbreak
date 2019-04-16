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
}
