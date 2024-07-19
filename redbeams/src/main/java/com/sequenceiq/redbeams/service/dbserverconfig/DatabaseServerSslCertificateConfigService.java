package com.sequenceiq.redbeams.service.dbserverconfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.util.TimeUtil;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerCertificateStatusV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslCertStatus;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerCertificateStatusV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerCertificateStatusV4Responses;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslConfigV4Response;
import com.sequenceiq.redbeams.converter.v4.databaseserver.DatabaseServerConfigToDatabaseServerV4ResponseConverter;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;

@Service
public class DatabaseServerSslCertificateConfigService {

    static final Long DEFAULT_WORKSPACE = 0L;

    static final int THREE_MONTH_IN_DAY = 90;

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseServerSslCertificateConfigService.class);

    @Inject
    private DatabaseServerConfigService databaseServerConfigService;

    @Inject
    private DatabaseServerConfigToDatabaseServerV4ResponseConverter databaseServerConfigToDatabaseServerV4ResponseConverter;

    @Inject
    private TimeUtil timeUtil;

    public DatabaseServerCertificateStatusV4Responses listDatabaseServersCertificateStatus(DatabaseServerCertificateStatusV4Request request) {
        DatabaseServerCertificateStatusV4Responses databaseServerCertificateStatusV4Responses = new DatabaseServerCertificateStatusV4Responses();
        Set<DatabaseServerConfig> databaseServerConfigs = databaseServerConfigService.findAll(DEFAULT_WORKSPACE,  request.getEnvironmentCrns());
        Map<String, SslCertStatus> sslCertStatus = getSslCertStatus(request.getEnvironmentCrns(), databaseServerConfigs);

        sslCertStatus.entrySet().forEach(actualEnvironment -> {
            LOGGER.info("Checking databases for environment {}", actualEnvironment.getKey());
            databaseServerCertificateStatusV4Responses.getResponses().add(getDatabaseServerCertificateStatusV4Response(actualEnvironment));
        });
        return databaseServerCertificateStatusV4Responses;
    }

    private DatabaseServerCertificateStatusV4Response getDatabaseServerCertificateStatusV4Response(Map.Entry<String, SslCertStatus> actualEnvironment) {
        DatabaseServerCertificateStatusV4Response databaseServerCertificateStatusV4Response = new DatabaseServerCertificateStatusV4Response();
        databaseServerCertificateStatusV4Response.setSslStatus(actualEnvironment.getValue());
        databaseServerCertificateStatusV4Response.setEnvironmentCrn(actualEnvironment.getKey());
        return databaseServerCertificateStatusV4Response;
    }

    private Map<String, SslCertStatus> getSslCertStatus(Set<String> environmentCrns, Set<DatabaseServerConfig> databaseServerConfigs) {
        Map<String, SslCertStatus> result = initializeResultMap(environmentCrns);
        for (DatabaseServerConfig databaseServerConfig : databaseServerConfigs) {
            SslConfigV4Response sslConfig = databaseServerConfigToDatabaseServerV4ResponseConverter.convertSslConfig(databaseServerConfig);
            LOGGER.info("Checking databases for environment {} and cluster {}", databaseServerConfig.getEnvironmentId(), databaseServerConfig.getClusterCrn());
            if (sslConfig.getSslMode().equals(SslMode.ENABLED)) {
                LOGGER.info("Ssl enabled for cluster {}", databaseServerConfig.getClusterCrn());
                long timestampsBefore = timeUtil.getTimestampThatDaysBeforeNow(THREE_MONTH_IN_DAY);

                if (sslConfig.getSslCertificateActiveVersion() != sslConfig.getSslCertificateHighestAvailableVersion()) {
                    LOGGER.info("Database certificate active version {} and highest version {} for cluster",
                            sslConfig.getSslCertificateActiveVersion(),
                            sslConfig.getSslCertificateHighestAvailableVersion(),
                            databaseServerConfig.getClusterCrn());
                    result.put(databaseServerConfig.getEnvironmentId(), SslCertStatus.OUTDATED);
                } else if (sslConfig.getSslCertificateExpirationDate() > timestampsBefore) {
                    LOGGER.info("Database expiration date {} which will expire less than 3 month {} for cluster",
                            sslConfig.getSslCertificateExpirationDate(),
                            databaseServerConfig.getClusterCrn());
                    result.put(databaseServerConfig.getEnvironmentId(), SslCertStatus.OUTDATED);
                }
            }
        }
        return result;
    }

    private Map<String, SslCertStatus> initializeResultMap(Set<String> environmentCrns) {
        Map<String, SslCertStatus> result = new HashMap<>();
        for (String environmentCrn : environmentCrns) {
            result.put(environmentCrn, SslCertStatus.UP_TO_DATE);
        }
        return result;
    }
}
