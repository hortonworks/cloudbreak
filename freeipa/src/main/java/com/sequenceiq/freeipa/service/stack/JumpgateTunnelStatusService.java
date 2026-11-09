package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2ParameterConstants.CCMV2_BACKEND_ID_FORMAT;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.workloadconnectivity.WorkloadConnectivityProto;
import com.sequenceiq.cloudbreak.ccmimpl.ccmv2.WorkloadConnectivityAdminClient;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

@Service
public class JumpgateTunnelStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JumpgateTunnelStatusService.class);

    @Inject
    private WorkloadConnectivityAdminClient workloadConnectivityAdminClient;

    public void logTunnelStatus(Stack stack, Collection<InstanceMetaData> instances) {
        if (!instances.isEmpty() && stack.getTunnel() != null && stack.getTunnel().useCcmV2Jumpgate()
                && StringUtils.isNotBlank(stack.getCcmV2AgentCrn())) {
            try {
                List<WorkloadConnectivityProto.TunnelStatus> tunnels = workloadConnectivityAdminClient
                        .listTunnelStatus(stack.getAccountId(), stack.getEnvironmentCrn())
                        .getTunnelsList();

                long agentTunnelCount = tunnels.stream()
                        .filter(tunnel -> tunnel.getAgentCrn().equals(stack.getCcmV2AgentCrn()))
                        .count();

                LOGGER.info("Jumpgate reports {} tunnel(s) for agent {} ({} in environment)",
                        agentTunnelCount, stack.getCcmV2AgentCrn(), tunnels.size());

                for (InstanceMetaData instanceMetaData : instances) {
                    String instanceId = instanceMetaData.getInstanceId();
                    findTunnelForInstance(tunnels, stack.getCcmV2AgentCrn(), instanceId).ifPresentOrElse(
                            tunnel -> {
                                long secondsSince = secondsSince(tunnel.getTimestamp());
                                String lastSeenStr = secondsSince < 0 ? "unknown" : secondsSince + "s ago";
                                LOGGER.info("Jumpgate tunnel status: instance {} last seen {}, agent {}, relay {}",
                                        instanceId, lastSeenStr, tunnel.getUserAgent(), tunnel.getRelayAddress());
                            },
                            () -> LOGGER.info("Jumpgate tunnel status: instance {} has no tunnel registered", instanceId));
                }
            } catch (Exception ex) {
                LOGGER.warn("Could not get jumpgate tunnel status for stack {}", stack.getResourceCrn(), ex);
            }
        }
    }

    private Optional<WorkloadConnectivityProto.TunnelStatus> findTunnelForInstance(List<WorkloadConnectivityProto.TunnelStatus> tunnels, String agentCrn,
            String instanceId) {
        String expectedId = String.format(CCMV2_BACKEND_ID_FORMAT, agentCrn, instanceId);
        return tunnels.stream()
                .filter(tunnel -> tunnel.getId().equals(expectedId)
                        || (tunnel.getAgentCrn().equals(agentCrn) && tunnel.getInstanceId().equals(instanceId)))
                .findFirst();
    }

    private long secondsSince(long timestampNanos) {
        if (timestampNanos <= 0) {
            return -1;
        }
        Instant lastSeen = Instant.ofEpochSecond(0, timestampNanos);
        return Duration.between(lastSeen, Instant.now()).getSeconds();
    }
}
