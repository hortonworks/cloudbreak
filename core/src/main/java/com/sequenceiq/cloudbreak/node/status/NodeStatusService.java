package com.sequenceiq.cloudbreak.node.status;

import javax.inject.Inject;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.node.health.client.CdpNodeStatusMonitorClient;
import com.sequenceiq.node.health.client.CdpNodeStatusMonitorClientException;

@Service
public class NodeStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeStatusService.class);

    @Inject
    private StackService stackService;

    @Inject
    private CdpNodeStatusMonitorClientFactory factory;

    public RPCResponse<NodeStatusProto.NodeStatusReport> getMeteringReport(Stack stack) {
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Retrieving metering report from the hosts of stack: {}", stack.getResourceCrn());
        try (CdpNodeStatusMonitorClient client = factory.getClient(stack, stack.getPrimaryGatewayInstance())) {
            return client.nodeMeteringReport();
        } catch (CdpNodeStatusMonitorClientException e) {
            LOGGER.warn("Node metering report failed", e);
            throw new CloudbreakServiceException("Could not get metering report from stack, reason: " + e.getMessage());
        }
    }

    public RPCResponse<NodeStatusProto.NodeStatusReport> getMeteringReport(Long stackId) {
        return getMeteringReport(stackService.getByIdWithGatewayInTransaction(stackId));
    }

    public RPCResponse<NodeStatusProto.NodeStatusReport> getNetworkReport(Stack stack) {
        LOGGER.debug("Retrieving network report from the hosts of stack: {}", stack.getResourceCrn());
        try (CdpNodeStatusMonitorClient client = factory.getClient(stack, stack.getPrimaryGatewayInstance())) {
            return client.nodeNetworkReport();
        } catch (CdpNodeStatusMonitorClientException e) {
            LOGGER.warn("Node network report failed", e);
            if (BooleanUtils.isFalse(e.getNginxResponseHeaderExists())) {
                throw new CloudbreakServiceException("Could not get network report from stack, nginx is unreachable, reason: " + e.getMessage());
            }
            throw new CloudbreakServiceException("Could not get network report from stack, reason: " + e.getMessage());
        }
    }

    public RPCResponse<NodeStatusProto.NodeStatusReport> getNetworkReport(Long stackId) {
        return getNetworkReport(stackService.getByIdWithGatewayInTransaction(stackId));
    }

    public RPCResponse<NodeStatusProto.NodeStatusReport> getServicesReport(Stack stack) {
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Retrieving services report from the hosts of stack: {}", stack.getResourceCrn());
        try (CdpNodeStatusMonitorClient client = factory.getClient(stack, stack.getPrimaryGatewayInstance())) {
            return client.nodeServicesReport();
        } catch (CdpNodeStatusMonitorClientException e) {
            LOGGER.warn("Node services report failed", e);
            throw new CloudbreakServiceException("Could not get services report from stack, reason: " + e.getMessage());
        }
    }

    public RPCResponse<NodeStatusProto.NodeStatusReport> getServicesReport(Long stackId) {
        return getServicesReport(stackService.getByIdWithGatewayInTransaction(stackId));
    }

    public RPCResponse<NodeStatusProto.SaltHealthReport> saltPing(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Retrieving salt ping report from the hosts of stack: {}", stack.getResourceCrn());
        try (CdpNodeStatusMonitorClient client = factory.getClient(stack, stack.getPrimaryGatewayInstance())) {
            return client.saltPing(false, false);
        } catch (CdpNodeStatusMonitorClientException e) {
            LOGGER.warn("Node salt ping failed", e);
            throw new CloudbreakServiceException("Could not get salt ping report from stack, reason: " + e.getMessage());
        }
    }

}
