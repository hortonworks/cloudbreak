package com.sequenceiq.cloudbreak.util;

import static java.util.Objects.requireNonNull;

/**
 * A host, port, and database name.
 */
public class HostAndPortAndDatabaseName {

    private final String hostAndPort;

    private final String databaseName;

    public HostAndPortAndDatabaseName(String host, int port, String databaseName) {
        hostAndPort = requireNonNull(host, "host is null") + ':'
                + requireNonNull(port, "port is null");
        this.databaseName = requireNonNull(databaseName, "databaseName is null");
    }

    /**
     * Returns the host and port separated by a colon.
     *
     * @return the host and port
     */
    public String getHostAndPort() {
        return hostAndPort;
    }

    /**
     * Returns the database name.
     *
     * @return the database name
     */
    public String getDatabaseName() {
        return databaseName;
    }
}
