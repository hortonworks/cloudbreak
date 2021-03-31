package com.sequenceiq.cloudbreak.node.status;

import javax.inject.Inject;

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

    public RPCResponse<NodeStatusProto.NodeStatusReport> getMeteringReport(Long stackId) {
        Stack stack = stackService.getByIdWithGatewayInTransaction(stackId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Retrieving metering report from the hosts of stack: {}", stack.getResourceCrn());
        CdpNodeStatusMonitorClient client = factory.getClient(stack, stack.getPrimaryGatewayInstance());
        try {
            return client.nodeMeteringReport();
        } catch (CdpNodeStatusMonitorClientException e) {
            throw new CloudbreakServiceException("Could not get metering report from stack.");
        }
    }

    public RPCResponse<NodeStatusProto.NodeStatusReport> getNetworkReport(Long stackId) {
        Stack stack = stackService.getByIdWithGatewayInTransaction(stackId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Retrieving network report from the hosts of stack: {}", stack.getResourceCrn());
        CdpNodeStatusMonitorClient client = factory.getClient(stack, stack.getPrimaryGatewayInstance());
        try {
            return client.nodeNetworkReport();
        } catch (CdpNodeStatusMonitorClientException e) {
            throw new CloudbreakServiceException("Could not get network report from stack.");
        }
    }

    public RPCResponse<NodeStatusProto.NodeStatusReport> getServicesReport(Long stackId) {
        Stack stack = stackService.getByIdWithGatewayInTransaction(stackId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Retrieving services report from the hosts of stack: {}", stack.getResourceCrn());
        CdpNodeStatusMonitorClient client = factory.getClient(stack, stack.getPrimaryGatewayInstance());
        try {
            return client.nodeServicesReport();
        } catch (CdpNodeStatusMonitorClientException e) {
            throw new CloudbreakServiceException("Could not get services report from stack.");
        }
    }

    public RPCResponse<NodeStatusProto.NodeStatusReport> getSystemMetrics(Long stackId) {
        Stack stack = stackService.getByIdWithGatewayInTransaction(stackId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Retrieving system metrics report from the hosts of stack: {}", stack.getResourceCrn());
        CdpNodeStatusMonitorClient client = factory.getClient(stack, stack.getPrimaryGatewayInstance());
        try {
            return client.systemMetricsReport();
        } catch (CdpNodeStatusMonitorClientException e) {
            throw new CloudbreakServiceException("Could not get system metrics report from stack.");
        }
    }

    public RPCResponse<NodeStatusProto.SaltHealthReport> getSaltReport(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Retrieving salt report from the hosts of stack: {}", stack.getResourceCrn());
        CdpNodeStatusMonitorClient client = factory.getClient(stack, stack.getPrimaryGatewayInstance());
        try {
            return client.saltReport();
        } catch (CdpNodeStatusMonitorClientException e) {
            throw new CloudbreakServiceException("Could not get salt report from stack.");
        }
    }

}
