package com.sequenceiq.cloudbreak.database;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.util.Pair;

public class JdbcUrlHostnamePortExtractor {

    private JdbcUrlHostnamePortExtractor() {
    }

    public static Pair<String, Integer> getHostnamePort(String jdbcUrl) {
        if (StringUtils.isEmpty(jdbcUrl)) {
            throw new IllegalStateException("JdbcUrl could not be empty String.");
        }
        int protocolDelimiterEndIndex = jdbcUrl.indexOf("//") + 2;
        String databaseAddress = jdbcUrl.substring(protocolDelimiterEndIndex, jdbcUrl.indexOf("/", protocolDelimiterEndIndex));
        String[] hostNameAndPortArray = databaseAddress.split(":");
        return Pair.of(hostNameAndPortArray[0], Integer.parseInt(hostNameAndPortArray[1]));
    }
}
