package com.sequenceiq.cloudbreak.core.bootstrap.service;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceStatus;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class ClusterServiceRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterServiceRunner.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private ClusterHostServiceRunner hostRunner;

    @Inject
    private GatewayConfigService gatewayConfigService;

    public void runAmbariServices(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Cluster cluster = stack.getCluster();
        Orchestrator orchestrator = stack.getOrchestrator();
        MDCBuilder.buildMdcContext(cluster);
        OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(orchestrator.getType());
        if (orchestratorType.hostOrchestrator()) {
            hostRunner.runAmbariServices(stack, cluster);
            String gatewayIp = gatewayConfigService.getPrimaryGatewayIp(stack);
            HttpClientConfig ambariClientConfig = buildAmbariClientConfig(stack, gatewayIp);
            clusterService.updateAmbariClientConfig(cluster.getId(), ambariClientConfig);
            for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaDataSet()) {
                instanceMetaDataService.updateInstanceStatus(instanceMetaData, InstanceStatus.SERVICES_RUNNING);
            }
        } else {
            LOGGER.info("Please implement {} orchestrator because it is not on classpath.", orchestrator.getType());
            throw new CloudbreakException(String.format("Please implement %s orchestrator because it is not on classpath.", orchestrator.getType()));
        }
    }

    public void updateSaltState(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Orchestrator orchestrator = stack.getOrchestrator();
        OrchestratorType orchestratorType = orchestratorTypeResolver.resolveType(orchestrator.getType());
        if (orchestratorType.containerOrchestrator()) {
            LOGGER.info("Container orchestrator is not supported for this action.");
        } else {
            Cluster cluster = clusterService.retrieveClusterByStackIdWithoutAuth(stack.getId());
            hostRunner.runAmbariServices(stack, cluster);
        }
    }

    public String changePrimaryGateway(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Orchestrator orchestrator = stack.getOrchestrator();
        if (orchestratorTypeResolver.resolveType(orchestrator.getType()).hostOrchestrator()) {
            return hostRunner.changePrimaryGateway(stack);
        }
        throw new CloudbreakException(String.format("Change primary gateway is not supported on orchestrator %s", orchestrator.getType()));
    }

    private HttpClientConfig buildAmbariClientConfig(Stack stack, String gatewayPublicIp) {
        return tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), gatewayPublicIp);
    }
}
