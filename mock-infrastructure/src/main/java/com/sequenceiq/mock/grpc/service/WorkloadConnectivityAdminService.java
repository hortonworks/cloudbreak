package com.sequenceiq.mock.grpc.service;

import static com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterConstants.CCMV2_BACKEND_ID_FORMAT;

import java.time.Instant;
import java.util.Objects;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto;
import com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiGrpc;
import com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityAdminApiProto;
import com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityProto;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.ccm.key.CcmResourceUtil;
import com.sequenceiq.mock.spi.SpiStoreService;

import io.grpc.stub.StreamObserver;

@Component
public class WorkloadConnectivityAdminService extends WorkloadConnectivityAdminApiGrpc.WorkloadConnectivityAdminApiImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkloadConnectivityAdminService.class);

    private static final String MOCK_USER_AGENT = "jumpgate-agent/mock";

    private static final String MOCK_RELAY_ADDRESS = "jumpgate-relay-0.mock";

    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    @Inject
    private CcmV2Service ccmV2Service;

    @Inject
    private SpiStoreService spiStoreService;

    @Override
    public void listTunnelStatus(WorkloadConnectivityAdminApiProto.ListTunnelStatusRequest request,
            StreamObserver<WorkloadConnectivityAdminApiProto.ListTunnelStatusResponse> responseObserver) {
        String environmentCrn = request.getEnvironmentCrn();
        WorkloadConnectivityAdminApiProto.ListTunnelStatusResponse.Builder response = WorkloadConnectivityAdminApiProto.ListTunnelStatusResponse.newBuilder();

        ccmV2Service.findAgentByEnvironmentCrn(environmentCrn).ifPresent(agent -> {
            String keyId = CcmResourceUtil.getKeyId(agent.getAgentCrn());
            spiStoreService.getAll().stream()
                    .filter(spiDto -> keyId.equals(CcmResourceUtil.getKeyId(spiDto.getMockUuid())))
                    .flatMap(spiDto -> spiDto.getVmMetaDataStatuses().stream())
                    .map(vm -> vm.getCloudVmInstanceStatus().getCloudInstance().getInstanceId())
                    .forEach(instanceId -> response.addTunnels(toTunnelStatus(agent, environmentCrn, instanceId)));
        });

        LOGGER.info("Mock listTunnelStatus called for environment {}", request.getEnvironmentCrn());
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    private WorkloadConnectivityProto.TunnelStatus toTunnelStatus(ClusterConnectivityManagementV2Proto.InvertingProxyAgent agent, String environmentCrn,
            String instanceId) {
        Instant now = Instant.now();

        return WorkloadConnectivityProto.TunnelStatus.newBuilder()
                .setId(String.format(CCMV2_BACKEND_ID_FORMAT, agent.getAgentCrn(), instanceId))
                .setAccountId(Objects.requireNonNull(Crn.fromString(agent.getAgentCrn())).getAccountId())
                .setEnvironmentCrn(environmentCrn)
                .setAgentCrn(agent.getAgentCrn())
                .setInstanceId(instanceId)
                .setUserAgent(MOCK_USER_AGENT)
                .setRelayAddress(MOCK_RELAY_ADDRESS)
                .setTimestamp(now.getEpochSecond() * NANOS_PER_SECOND + now.getNano())
                .build();
    }
}
