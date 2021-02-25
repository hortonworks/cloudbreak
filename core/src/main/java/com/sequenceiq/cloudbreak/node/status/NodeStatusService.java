package com.sequenceiq.cloudbreak.node.status;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.converter.v4.node.status.HealthReportToNodeStatusResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.node.status.SaltReportToNodeStatusResponseConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.node.status.response.NodeStatusResponse;
import com.sequenceiq.common.api.node.status.response.SaltStatusResponse;
import com.sequenceiq.node.health.client.CdpNodeStatusMonitorClient;
import com.sequenceiq.node.health.client.CdpNodeStatusMonitorClientException;
import com.sequenceiq.node.health.client.model.HealthReport;
import com.sequenceiq.node.health.client.model.SaltReport;

@Service
public class NodeStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeStatusService.class);

    @Inject
    private StackService stackService;

    @Inject
    private CdpNodeStatusMonitorClientFactory factory;

    @Inject
    private HealthReportToNodeStatusResponseConverter converter;

    @Inject
    private SaltReportToNodeStatusResponseConverter saltConverter;

    public NodeStatusResponse getMeteringReport(String stackCrn) {
        Stack stack = stackService.getByCrn(stackCrn);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Retrieving metering report from the hosts of stack: {}", stackCrn);
        CdpNodeStatusMonitorClient client = factory.getClient(stack, stack.getPrimaryGatewayInstance());
        try {
            RPCResponse<HealthReport> response = client.nodeMeteringReport();
            return converter.convert(response.getResult());
        } catch (CdpNodeStatusMonitorClientException e) {
            throw new CloudbreakServiceException("Could not get metering report from stack.");
        }
    }

    public NodeStatusResponse getNetworkReport(String stackCrn) {
        Stack stack = stackService.getByCrn(stackCrn);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Retrieving network report from the hosts of stack: {}", stackCrn);
        CdpNodeStatusMonitorClient client = factory.getClient(stack, stack.getPrimaryGatewayInstance());
        try {
            RPCResponse<HealthReport> response = client.nodeNetworkReport();
            return converter.convert(response.getResult());
        } catch (CdpNodeStatusMonitorClientException e) {
            throw new CloudbreakServiceException("Could not get network report from stack.");
        }
    }

    public NodeStatusResponse getServicesReport(String stackCrn) {
        Stack stack = stackService.getByCrn(stackCrn);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Retrieving services report from the hosts of stack: {}", stackCrn);
        CdpNodeStatusMonitorClient client = factory.getClient(stack, stack.getPrimaryGatewayInstance());
        try {
            RPCResponse<HealthReport> response = client.nodeServicesReport();
            return converter.convert(response.getResult());
        } catch (CdpNodeStatusMonitorClientException e) {
            throw new CloudbreakServiceException("Could not get services report from stack.");
        }
    }

    public SaltStatusResponse getSaltReport(String stackCrn) {
        Stack stack = stackService.getByCrn(stackCrn);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Retrieving salt report from the hosts of stack: {}", stackCrn);
        CdpNodeStatusMonitorClient client = factory.getClient(stack, stack.getPrimaryGatewayInstance());
        try {
            RPCResponse<SaltReport> response = client.saltReport();
            return saltConverter.convert(response.getResult());
        } catch (CdpNodeStatusMonitorClientException e) {
            throw new CloudbreakServiceException("Could not get salt report from stack.");
        }
    }
}
