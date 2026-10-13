package com.sequenceiq.cloudbreak.core.flow2.chain.util;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isHostsStartRolesSupported;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hbase.HbaseRoles;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.phoenix.PhoenixRoles;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn.YarnRoles;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;

@Component
public class RepairServiceRestartDecider {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepairServiceRestartDecider.class);

    private static final Set<String> WORKER_ROLES_NOT_REQUIRING_CLUSTER_RESTART =
            Set.of(HdfsRoles.DATANODE, YarnRoles.NODEMANAGER, HbaseRoles.REGIONSERVER, PhoenixRoles.PHOENIX_QUERY_SERVER);

    private static final RepairServiceRestartPolicy NEVER_RESTART = repairedHostGroups -> false;

    private static final RepairServiceRestartPolicy ALWAYS_RESTART = repairedHostGroups -> true;

    @Value("${cb.upgrade.repair.skip.worker.restart.enabled}")
    private boolean skipWorkerRestartEnabled;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    public RepairServiceRestartPolicy restartPolicyFor(ClusterRepairTriggerEvent event, StackDto stackDto) {
        if (!event.isRestartServices()) {
            return NEVER_RESTART;
        }
        if (event.isRollingRestartEnabled()) {
            LOGGER.info("Keeping the service restart flag for every repaired host group because the restart is skipped for rolling restart anyway.");
            return ALWAYS_RESTART;
        }
        if (!skipWorkerRestartEnabled || !event.isUpgrade()) {
            LOGGER.info("Keeping the service restart for every repaired host group. Skip enabled: {}, upgrade: {}",
                    skipWorkerRestartEnabled, event.isUpgrade());
            return ALWAYS_RESTART;
        }
        if (!rolesOnReplacedHostsAreStartedByClouderaManager(stackDto)) {
            LOGGER.info("Keeping the service restart for every repaired host group because Cloudera Manager does not start the roles on the replaced hosts.");
            return ALWAYS_RESTART;
        }
        return nonGatewayRolesByHostGroup(stackDto)
                .map(this::skipRestartForWorkerOrClientOnlyHostGroups)
                .orElse(ALWAYS_RESTART);
    }

    private boolean rolesOnReplacedHostsAreStartedByClouderaManager(StackDto stackDto) {
        ClouderaManagerRepo clouderaManagerRepo = clusterComponentConfigProvider.getClouderaManagerRepoDetails(stackDto.getCluster().getId());
        if (clouderaManagerRepo == null) {
            LOGGER.warn("Cloudera Manager repo details are not available for cluster '{}'.", stackDto.getCluster().getId());
            return false;
        }
        return isHostsStartRolesSupported(clouderaManagerRepo);
    }

    private Optional<Map<String, Set<String>>> nonGatewayRolesByHostGroup(StackDto stackDto) {
        String deployedTemplateText = stackDto.getBlueprintJsonText();
        if (StringUtils.isBlank(deployedTemplateText)) {
            LOGGER.warn("There is no cluster template text for cluster '{}', the repaired host groups cannot be classified.", stackDto.getCluster().getId());
            return Optional.empty();
        }
        return Optional.of(cmTemplateProcessorFactory.get(deployedTemplateText).getNonGatewayComponentsByHostGroup());
    }

    private RepairServiceRestartPolicy skipRestartForWorkerOrClientOnlyHostGroups(Map<String, Set<String>> nonGatewayRolesByHostGroup) {
        return repairedHostGroups -> {
            if (containsWorkerOrClientOnlyHostGroups(repairedHostGroups, nonGatewayRolesByHostGroup)) {
                LOGGER.info("Skipping the service restart because the repaired host groups run worker or client roles only: {}", repairedHostGroups);
                return false;
            }
            LOGGER.info("Keeping the service restart because the repaired host groups run roles that need it: {}", repairedHostGroups);
            return true;
        };
    }

    private boolean containsWorkerOrClientOnlyHostGroups(Set<String> repairedHostGroups, Map<String, Set<String>> nonGatewayRolesByHostGroup) {
        return repairedHostGroups.stream().allMatch(hostGroup -> isWorkerOrClientOnlyHostGroup(hostGroup, nonGatewayRolesByHostGroup));
    }

    private boolean isWorkerOrClientOnlyHostGroup(String hostGroup, Map<String, Set<String>> nonGatewayRolesByHostGroup) {
        Set<String> nonGatewayRoles = nonGatewayRolesByHostGroup.get(hostGroup.toLowerCase(Locale.ROOT));
        if (nonGatewayRoles == null) {
            LOGGER.warn("Host group '{}' is not present in the cluster template, treating it as a host group that needs the service restart.", hostGroup);
            return false;
        }
        boolean clientRolesOnly = nonGatewayRoles.isEmpty();
        return clientRolesOnly || WORKER_ROLES_NOT_REQUIRING_CLUSTER_RESTART.containsAll(nonGatewayRoles);
    }
}
