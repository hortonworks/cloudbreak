package com.sequenceiq.cloudbreak.core.cluster;

import static com.sequenceiq.cloudbreak.cluster.model.CMConfigUpdateStrategy.FALLBACK_TO_ROLLCONFIG;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Table;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.RdsSettingsMigrationService;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.service.ClusterServicesRestartService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class ClusterStartHandlerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStartHandlerService.class);

    @Inject
    private ClusterApiConnectors apiConnectors;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private ClusterServicesRestartService clusterServicesRestartService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private RdsSettingsMigrationService rdsSettingsMigrationService;

    @Inject
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    public void startCluster(Stack stack, CmTemplateProcessor blueprintProcessor, boolean datahubRefreshNeeded) throws Exception {
        Optional<SdxBasicView> sdxBasicView = platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(stack.getEnvironmentCrn());
        if (sdxBasicView.isPresent()) {
            // let's update config only in case of non-CDL form factor for now
            if (clusterServicesRestartService.isRemoteDataContextRefreshNeeded(stack, sdxBasicView.get()) || datahubRefreshNeeded) {
                // refresh DH after DL resize
                clusterServicesRestartService.refreshClusterOnStart(stack, sdxBasicView.get(), blueprintProcessor);
            } else {
                // refresh DH after flexible server upgrade
                startAndRefreshSharedRdsConfigIfNeeded(stack);
            }
        }
    }

    private void startAndRefreshSharedRdsConfigIfNeeded(Stack stack) throws Exception {
        LOGGER.info("Starting cluster manager and its agents on {} cluster", stack.getName());
        apiConnectors.getConnector(stack).startClusterManagerAndAgents();
        if (StackType.WORKLOAD.equals(stack.getType())) {
            refreshSharedRdsConfigIfNeeded(stack);
        }
        LOGGER.info("Starting services on {} cluster", stack.getName());
        apiConnectors.getConnector(stack).startCluster(true);
    }

    // if this logic is needed for CDL, remote data context should be used to compare current config of DH and CDL
    private void refreshSharedRdsConfigIfNeeded(Stack stack) {
        try {
            Predicate<RDSConfig> cmServicePredicate = this::isClouderaManagerService;
            Predicate<RDSConfig> sharedDbPredicate = this::isSharedDatabase;
            Set<RDSConfig> rdsConfigs = rdsSettingsMigrationService.collectRdsConfigs(stack.getClusterId(), cmServicePredicate.and(sharedDbPredicate));
            if (!rdsConfigs.isEmpty()) {
                LOGGER.info("Update rds configs which are using the datalake's shared database for services {}",
                        rdsConfigs.stream().map(RDSConfig::getType).collect(Collectors.toSet()));
                Table<String, String, String> cmServiceConfigs = rdsSettingsMigrationService.collectCMServiceConfigs(rdsConfigs);
                rdsSettingsMigrationService.updateCMServiceConfigs(stack, cmServiceConfigs, FALLBACK_TO_ROLLCONFIG, false);
            } else {
                LOGGER.info("No rds config update is needed on datahub {}", stack.getName());
            }
        } catch (Exception ex) {
            LOGGER.warn("Exception happened during rds config update during cluster start." +
                    " The start flow will be continued, maybe manual changes will be needed on CM UI.", ex);
        }
    }

    public void handleStopStartScalingFeature(Stack stack, CmTemplateProcessor blueprintProcessor) {
        if (stackUtil.stopStartScalingEntitlementEnabled(stack)) {
            Set<String> computeGroups = getComputeHostGroups(blueprintProcessor);
            if (computeGroups.isEmpty()) {
                return;
            }
            List<String> decommissionedHostsFromCM = apiConnectors.getConnector(stack).clusterStatusService().getDecommissionedHostsFromCM();
            if (decommissionedHostsFromCM.isEmpty()) {
                return;
            }
            Set<String> decommissionedComputeHosts = new HashSet<>();
            for (String group : computeGroups) {
                String groupWithPrefix = '-' + group;
                for (String hostName : decommissionedHostsFromCM) {
                    if (hostName.contains(groupWithPrefix)) {
                        decommissionedComputeHosts.add(hostName);
                    }
                }
            }
            if (!decommissionedComputeHosts.isEmpty()) {
                apiConnectors.getConnector(stack).clusterCommissionService().recommissionHosts(new ArrayList<>(decommissionedComputeHosts));
            }
        }
    }

    public CmTemplateProcessor getCmTemplateProcessor(Stack stack) {
        return cmTemplateProcessorFactory.get(stack.getBlueprintJsonText());
    }

    private Set<String> getComputeHostGroups(CmTemplateProcessor blueprintProcessor) {
        Versioned blueprintVersion = () -> blueprintProcessor.getVersion().get();
        return blueprintProcessor.getComputeHostGroups(blueprintVersion);
    }

    private boolean isClouderaManagerService(RDSConfig rdsConfig) {
        return !DatabaseType.CLOUDERA_MANAGER.name().equals(rdsConfig.getType());
    }

    private boolean isSharedDatabase(RDSConfig rdsConfig) {
        return rdsConfigService.countOfClustersUsingResource(rdsConfig) > 1;
    }
}
