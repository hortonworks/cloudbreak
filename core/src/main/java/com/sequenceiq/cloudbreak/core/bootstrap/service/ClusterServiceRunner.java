package com.sequenceiq.cloudbreak.core.bootstrap.service;

import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.gateway.GatewayService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.GatewayView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@Component
public class ClusterServiceRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterServiceRunner.class);

    @Inject
    private StackDtoService stackDtoService;

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
    private GatewayService gatewayService;

    public void runAmbariServices(StackDto stackDto) {
        ClusterView cluster = stackDto.getCluster();

        generateGatewaySignKeys(stackDto.getGateway());

        MDCBuilder.buildMdcContext(cluster);
        hostRunner.runClusterServices(stackDto, Map.of());
        for (InstanceMetadataView instanceMetaData : stackDto.getRunningInstanceMetaDataSet()) {
            instanceMetaDataService.updateInstanceStatus(instanceMetaData, InstanceStatus.SERVICES_RUNNING);
        }

    }

    private void generateGatewaySignKeys(GatewayView gateway) {
        if (Objects.nonNull(gateway)) {
            gatewayService.generateAndUpdateSignKeys(gateway);
        }
    }

    public String updateAmbariClientConfig(StackDto stackDto) {
        String gatewayIp = gatewayConfigService.getPrimaryGatewayIp(stackDto);
        HttpClientConfig ambariClientConfig = buildAmbariClientConfig(stackDto.getStack(), gatewayIp);
        clusterService.updateAmbariClientConfig(stackDto.getCluster().getId(), ambariClientConfig);
        return gatewayIp;
    }

    public void redeployGatewayCertificate(Long stackId) {
        StackDto stackDto = stackDtoService.getById(stackId);
        hostRunner.redeployGatewayCertificate(stackDto);
    }

    public void redeployGatewayPillar(Long stackId) {
        StackDto stack = stackDtoService.getById(stackId);
        hostRunner.redeployGatewayPillarOnly(stack);
    }

    public void redeployStates(Long stackId) {
        StackDto stack = stackDtoService.getById(stackId);
        hostRunner.redeployStates(stack);
    }

    public String changePrimaryGateway(Long stackId) throws CloudbreakException {
        StackDto stackDto = stackDtoService.getById(stackId);
        return hostRunner.changePrimaryGateway(stackDto);
    }

    private HttpClientConfig buildAmbariClientConfig(StackView stack, String gatewayPublicIp) {
        return tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), gatewayPublicIp, stack.getCloudPlatform());
    }
}
