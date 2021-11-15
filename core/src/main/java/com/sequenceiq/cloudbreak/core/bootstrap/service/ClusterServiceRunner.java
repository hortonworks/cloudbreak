package com.sequenceiq.cloudbreak.core.bootstrap.service;

import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.gateway.GatewayService;
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
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private ClusterHostServiceRunner hostRunner;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private GatewayConvertUtil convertUtil;

    @Inject
    private GatewayService gatewayService;

    public void runAmbariServices(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Cluster cluster = clusterService.getById(stack.getCluster().getId());

        generateGatewaySignKeys(cluster);

        MDCBuilder.buildMdcContext(cluster);
        hostRunner.runClusterServices(stack, cluster, Map.of());
        updateAmbariClientConfig(stack, cluster);
        for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaDataSet()) {
            instanceMetaDataService.updateInstanceStatus(instanceMetaData, InstanceStatus.SERVICES_RUNNING);
        }

    }

    private void generateGatewaySignKeys(Cluster cluster) {
        Gateway gateway = cluster.getGateway();
        if (Objects.nonNull(gateway)) {
            convertUtil.generateSignKeys(gateway);
            gatewayService.save(gateway);
        }
    }

    public void updateAmbariClientConfig(Stack stack, Cluster cluster) {
        String gatewayIp = gatewayConfigService.getPrimaryGatewayIp(stack);
        HttpClientConfig ambariClientConfig = buildAmbariClientConfig(stack, gatewayIp);
        Cluster updatedCluster = clusterService.updateAmbariClientConfig(cluster.getId(), ambariClientConfig);
        stack.setCluster(updatedCluster);
    }

    public void redeployGatewayCertificate(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Cluster cluster = clusterService.retrieveClusterByStackIdWithoutAuth(stack.getId())
                .orElseThrow(NotFoundException.notFound("cluster", stack.getId()));
        hostRunner.redeployGatewayCertificate(stack, cluster);
    }

    public String changePrimaryGateway(Long stackId) throws CloudbreakException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        return hostRunner.changePrimaryGateway(stack);
    }

    private HttpClientConfig buildAmbariClientConfig(Stack stack, String gatewayPublicIp) {
        return tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), gatewayPublicIp, stack.cloudPlatform());
    }
}
