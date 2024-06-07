package com.sequenceiq.cloudbreak.database;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

public class RdsIamAuthBasedHikariDataSource extends HikariDataSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsIamAuthBasedHikariDataSource.class);

    public RdsIamAuthBasedHikariDataSource(HikariConfig configuration) {
        super(configuration);
    }

    @Override
    public String getPassword() {
        return getToken();
    }

    private String getToken() {
        Instant started = Instant.now();
        Pair<String, Integer> hostNameAndPort = JdbcUrlHostnamePortExtractor.getHostnamePort(getJdbcUrl());

        Region region = new DefaultAwsRegionProviderChain().getRegion();
        try (RdsClient rdsClient = RdsClient.builder()
                .region(region)
                .build()) {

            RdsUtilities  rdsUtilities = rdsClient.utilities();

            GenerateAuthenticationTokenRequest request = GenerateAuthenticationTokenRequest.builder()
                    .username(getUsername())
                    .hostname(hostNameAndPort.getFirst())
                    .port(hostNameAndPort.getSecond())
                    .build();

            String rdsAuthToken = rdsUtilities.generateAuthenticationToken(request);
            long executionTime = Instant.now().toEpochMilli() - started.toEpochMilli();
            LOGGER.debug("Acquire RDS authentication token took {}ms", executionTime);
            return rdsAuthToken;
        }
    }
}
