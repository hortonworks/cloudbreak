package com.sequenceiq.redbeams.service.dbserverconfig;

import static com.sequenceiq.cloudbreak.auth.crn.Crn.ResourceType.CLUSTER;
import static com.sequenceiq.cloudbreak.auth.crn.Crn.ResourceType.DATALAKE;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerSslCertificateConfigService.SslCertStatusType.ENVIRONMENT;
import static com.sequenceiq.redbeams.service.dbserverconfig.DatabaseServerSslCertificateConfigService.SslCertStatusType.STACK;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.domain.SslCertStatus;
import com.sequenceiq.cloudbreak.util.TimeUtil;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.ClusterDatabaseServerCertificateStatusV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerCertificateStatusV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.ClusterDatabaseServerCertificateStatusV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.ClusterDatabaseServerCertificateStatusV4Responses;
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

    public DatabaseServerCertificateStatusV4Responses listDatabaseServersCertificateStatus(
            DatabaseServerCertificateStatusV4Request request, String accountId) {
        DatabaseServerCertificateStatusV4Responses databaseServerCertificateStatusV4Responses = new DatabaseServerCertificateStatusV4Responses();
        Set<DatabaseServerConfig> databaseServerConfigs = databaseServerConfigService.findAllByEnvironmentCrns(accountId,  request.getEnvironmentCrns());
        Map<String, SslCertStatus> sslCertStatus = getSslCertStatus(request.getEnvironmentCrns(), databaseServerConfigs, ENVIRONMENT);

        sslCertStatus.entrySet().forEach(actualEnvironment -> {
            LOGGER.info("Checking databases for environment {}", actualEnvironment.getKey());
            databaseServerCertificateStatusV4Responses.getResponses().add(getDatabaseServerCertificateStatusV4Response(actualEnvironment));
        });
        return databaseServerCertificateStatusV4Responses;
    }

    public ClusterDatabaseServerCertificateStatusV4Responses listDatabaseServersCertificateStatus(
            ClusterDatabaseServerCertificateStatusV4Request request, String accountId) {
        ClusterDatabaseServerCertificateStatusV4Responses databaseServerCertificateStatusV4Responses = new ClusterDatabaseServerCertificateStatusV4Responses();
        Set<DatabaseServerConfig> databaseServerConfigs = databaseServerConfigService.findAllByClusterCrns(accountId, request.getCrns());
        Map<String, SslCertStatus> sslCertStatus = getSslCertStatus(request.getCrns(), databaseServerConfigs, STACK);

        sslCertStatus.entrySet().forEach(actualCluster -> {
            LOGGER.info("Checking databases for cluster {}", actualCluster.getKey());
            databaseServerCertificateStatusV4Responses.getResponses().add(getStackDatabaseServerCertificateStatusV4Response(actualCluster));
        });
        return databaseServerCertificateStatusV4Responses;
    }

    private DatabaseServerCertificateStatusV4Response getDatabaseServerCertificateStatusV4Response(Map.Entry<String, SslCertStatus> actualEnvironment) {
        DatabaseServerCertificateStatusV4Response databaseServerCertificateStatusV4Response = new DatabaseServerCertificateStatusV4Response();
        databaseServerCertificateStatusV4Response.setSslStatus(actualEnvironment.getValue());
        databaseServerCertificateStatusV4Response.setEnvironmentCrn(actualEnvironment.getKey());
        return databaseServerCertificateStatusV4Response;
    }

    private ClusterDatabaseServerCertificateStatusV4Response getStackDatabaseServerCertificateStatusV4Response(Map.Entry<String, SslCertStatus> actualCluster) {
        ClusterDatabaseServerCertificateStatusV4Response databaseServerCertificateStatusV4Response = new ClusterDatabaseServerCertificateStatusV4Response();
        databaseServerCertificateStatusV4Response.setSslStatus(actualCluster.getValue());
        databaseServerCertificateStatusV4Response.setCrn(actualCluster.getKey());
        return databaseServerCertificateStatusV4Response;
    }

    private Map<String, SslCertStatus> getSslCertStatus(Set<String> crns, Set<DatabaseServerConfig> databaseServerConfigs,
        SslCertStatusType sslCertStatusType) {
        Map<String, SslCertStatus> result = initializeResultMap(crns);
        for (DatabaseServerConfig databaseServerConfig : databaseServerConfigs) {
            if (isAwsDBStack(databaseServerConfig)) {
                SslConfigV4Response sslConfig = databaseServerConfigToDatabaseServerV4ResponseConverter.convertSslConfig(databaseServerConfig);
                LOGGER.info("Checking databases for environment {} and cluster {}",
                        databaseServerConfig.getEnvironmentId(),
                        databaseServerConfig.getClusterCrn());
                if (sslConfig.getSslMode().equals(SslMode.ENABLED) && isDataHubOrDatalakeConfig(databaseServerConfig)) {
                    LOGGER.info("Ssl enabled for cluster {}", databaseServerConfig.getClusterCrn());
                    long certExpirationDeadLine = timeUtil.getTimestampThatDaysAfterNow(THREE_MONTH_IN_DAY);
                    if (sslConfig.getSslCertificateActiveVersion() != sslConfig.getSslCertificateHighestAvailableVersion()) {
                        LOGGER.info("Database certificate active version {} and highest version {} for cluster {} and environment {}",
                                sslConfig.getSslCertificateActiveVersion(),
                                sslConfig.getSslCertificateHighestAvailableVersion(),
                                databaseServerConfig.getClusterCrn(),
                                databaseServerConfig.getEnvironmentId());
                        addOutDatedEntry(result, sslCertStatusType, databaseServerConfig);
                    } else if (sslConfig.getSslCertificateExpirationDate() <= certExpirationDeadLine) {
                        LOGGER.info("Database expiration date {} which will expire less than 3 month for cluster {} and environment {}",
                                sslConfig.getSslCertificateExpirationDate(),
                                databaseServerConfig.getClusterCrn(),
                                databaseServerConfig.getEnvironmentId());
                        addOutDatedEntry(result, sslCertStatusType, databaseServerConfig);
                    }
                }
            }
        }
        LOGGER.info("Database server certificate status: {}", result);
        return result;
    }

    private void addOutDatedEntry(Map<String, SslCertStatus> result, SslCertStatusType sslCertStatusType,
        DatabaseServerConfig databaseServerConfig) {
        if (ENVIRONMENT.equals(sslCertStatusType)) {
            result.put(databaseServerConfig.getEnvironmentId(), SslCertStatus.OUTDATED);
        } else {
            result.put(databaseServerConfig.getClusterCrn(), SslCertStatus.OUTDATED);
        }
    }

    private boolean isDataHubOrDatalakeConfig(DatabaseServerConfig databaseServerConfig) {
        Crn crn = Crn.fromString(databaseServerConfig.getClusterCrn());
        return isDatalake(crn) || isDatahub(crn);
    }

    private boolean isDatahub(Crn crn) {
        return CLUSTER.equals(crn.getResourceType()) && Crn.Service.DATAHUB.equals(crn.getService());
    }

    private boolean isDatalake(Crn crn) {
        return DATALAKE.equals(crn.getResourceType()) && Crn.Service.DATALAKE.equals(crn.getService());
    }

    private boolean isAwsDBStack(DatabaseServerConfig databaseServerConfig) {
        return databaseServerConfig.getDbStack().isPresent() && AWS.name().equals(databaseServerConfig.getDbStack().get().getCloudPlatform());
    }

    private Map<String, SslCertStatus> initializeResultMap(Set<String> crns) {
        Map<String, SslCertStatus> result = new HashMap<>();
        for (String crn : crns) {
            result.put(crn, SslCertStatus.UP_TO_DATE);
        }
        return result;
    }

    enum SslCertStatusType {
        ENVIRONMENT, STACK
    }
}
