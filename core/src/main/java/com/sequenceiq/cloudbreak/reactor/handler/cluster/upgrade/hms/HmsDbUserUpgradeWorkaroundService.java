package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.hms;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.WORKLOAD;
import static com.sequenceiq.cloudbreak.cluster.model.CMConfigUpdateStrategy.FALLBACK_TO_ROLLCONFIG;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_PASSWORD;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_USER;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeRequest;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;

@Service
public class HmsDbUserUpgradeWorkaroundService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HmsDbUserUpgradeWorkaroundService.class);

    private static final String HMS_SERVICE_TYPE = "HIVE";

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    public void switchToSdxHmsDbUserBeforeUpgradeIfNeeded(StackDtoDelegate stack, ClusterUpgradeRequest request, Optional<String> rdc) {
        try {
            if (!request.isPatchUpgrade() && !request.getUpgradeCandidateProducts().isEmpty() && WORKLOAD.equals(stack.getType())) {
                ClusterApi connector = clusterApiConnectors.getConnector(stack);
                boolean hmsPresent = connector.clusterModificationService().isServicePresent(stack.getName(), HMS_SERVICE_TYPE);
                if (hmsPresent) {
                    RDSConfig hmsRdsConfig = getDatahubHmsRdsConfig(stack);
                    // it means DH has its own HMS DB user
                    if (hmsRdsConfig.getClusters().size() == 1) {
                        DatabaseCredentials datalakeHmsCreds = getDatalakeHmsCreds(stack, rdc);
                        LOGGER.info("Update HMS DB credentials config to use HMS DB user of Data Lake in order to execute runtime upgrade successfully!");
                        connector.clusterModificationService().updateConfigWithoutRestart(getUpdateConfigTable(datalakeHmsCreds), FALLBACK_TO_ROLLCONFIG);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to switch to DL's HMS RDS user needed to execute runtime upgrade sucessfully.");
            throw new CloudbreakServiceException(e);
        }
    }

    public void switchBackToOriginalHmsDbUserIfNeeded(StackDtoDelegate stack, ClusterUpgradeRequest request) {
        try {
            if (!request.isPatchUpgrade() && !request.getUpgradeCandidateProducts().isEmpty() && WORKLOAD.equals(stack.getType())) {
                ClusterApi connector = clusterApiConnectors.getConnector(stack);
                boolean hmsPresent = connector.clusterModificationService().isServicePresent(stack.getName(), HMS_SERVICE_TYPE);
                if (hmsPresent) {
                    RDSConfig hmsRdsConfig = getDatahubHmsRdsConfig(stack);
                    // it means DH has its own HMS DB user
                    if (hmsRdsConfig.getClusters().size() == 1) {
                        DatabaseCredentials datahubHmsCreds = getDatahubHmsRdsCreds(hmsRdsConfig);
                        LOGGER.info("Update HMS DB credentials config to use HMS DB user of Data Hub again!");
                        connector.clusterModificationService().updateConfig(getUpdateConfigTable(datahubHmsCreds), FALLBACK_TO_ROLLCONFIG);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to switch back to DH's HMS RDS user.");
            throw new CloudbreakServiceException(e);
        }
    }

    private static Table<String, String, String> getUpdateConfigTable(DatabaseCredentials hmsCreds) {
        Table<String, String, String> updateConfig = HashBasedTable.create();
        updateConfig.put(HMS_SERVICE_TYPE, HIVE_METASTORE_DATABASE_USER, hmsCreds.dbUser());
        updateConfig.put(HMS_SERVICE_TYPE, HIVE_METASTORE_DATABASE_PASSWORD, hmsCreds.dbPassword());
        return updateConfig;
    }

    private DatabaseCredentials getDatalakeHmsCreds(StackDtoDelegate stack, Optional<String> rdc) {
        SdxBasicView sdxBasicView = platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(stack.getEnvironmentCrn()).orElseThrow();
        Map<String, String> sdxHmsConfig = platformAwareSdxConnector.getHmsServiceConfig(sdxBasicView.crn(), rdc);
        String sdxHmsDatabaseUser = sdxHmsConfig.get(HIVE_METASTORE_DATABASE_USER);
        String sdxHmsDatabasePassword = sdxHmsConfig.get(HIVE_METASTORE_DATABASE_PASSWORD);
        return new DatabaseCredentials(sdxHmsDatabaseUser, sdxHmsDatabasePassword);
    }

    private DatabaseCredentials getDatahubHmsRdsCreds(RDSConfig hmsRdsConfig) {
        return new DatabaseCredentials(hmsRdsConfig.getConnectionUserName(), hmsRdsConfig.getConnectionPassword());
    }

    private RDSConfig getDatahubHmsRdsConfig(StackDtoDelegate stack) {
        return rdsConfigService.findByClusterId(stack.getCluster().getId())
                .stream()
                .filter(rds -> StringUtils.equals(rds.getType(), DatabaseType.HIVE.name()))
                .findFirst()
                .orElseThrow();
    }

    private record DatabaseCredentials(String dbUser, String dbPassword) {
    }
}
