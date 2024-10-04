package com.sequenceiq.cloudbreak.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class RdsIamAuthBasedHikariDataSource extends HikariDataSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsIamAuthBasedHikariDataSource.class);

    private final RdsIamAuthenticationTokenProvider authenticationTokenProvider;

    public RdsIamAuthBasedHikariDataSource(HikariConfig configuration, RdsIamAuthenticationTokenProvider authenticationTokenProvider) {
        super(configuration);
        this.authenticationTokenProvider = authenticationTokenProvider;
    }

    @Override
    public String getPassword() {
        String token;
        String username = getUsername();
        LOGGER.debug("Acquiring IAM authentication token for RDS with user: '{}'", username);
        if (authenticationTokenProvider != null) {
            token = authenticationTokenProvider.getToken(getJdbcUrl(), username);
        } else {
            LOGGER.debug("The 'getPassword()' is called from Super before dependency injection");
            token = RdsIamAuthenticationTokenProvider.getTokenUnCached(getJdbcUrl(), username);
        }
        return token;
    }
}
