package com.sequenceiq.cloudbreak.database;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

@Service
public class RdsIamAuthenticationTokenProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(RdsIamAuthenticationTokenProvider.class);

    public static String getTokenUnCached(String jdbcUrl, String userName) {
        Instant started = Instant.now();
        Pair<String, Integer> hostNameAndPort = JdbcUrlHostnamePortExtractor.getHostnamePort(jdbcUrl);

        Region region = new DefaultAwsRegionProviderChain().getRegion();
        try (RdsClient rdsClient = RdsClient.builder()
                .region(region)
                .build()) {

            RdsUtilities rdsUtilities = rdsClient.utilities();

            GenerateAuthenticationTokenRequest request = GenerateAuthenticationTokenRequest.builder()
                    .username(userName)
                    .hostname(hostNameAndPort.getFirst())
                    .port(hostNameAndPort.getSecond())
                    .build();

            String rdsAuthToken = rdsUtilities.generateAuthenticationToken(request);
            long executionTime = Instant.now().toEpochMilli() - started.toEpochMilli();
            LOGGER.debug("Acquire RDS authentication token took {}ms", executionTime);
            return rdsAuthToken;
        }
    }

    @Cacheable(cacheNames = "rdsIamAuthenticationTokenCache", key = "{ #jdbcUrl, #userName }", sync = true)
    public String getToken(String jdbcUrl, String userName) {
        return getTokenUnCached(jdbcUrl, userName);
    }
}
