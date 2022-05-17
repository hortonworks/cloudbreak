package com.sequenceiq.freeipa.service.stack;

import java.net.MalformedURLException;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaNodeStatusClientFactory;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.node.health.client.CdpNodeStatusMonitorClient;
import com.sequenceiq.node.health.client.CdpNodeStatusMonitorClientException;
import com.sequenceiq.node.health.client.model.CdpNodeStatusRequest;
import com.sequenceiq.node.health.client.model.CdpNodeStatuses;

@Service
public class FreeIpaNodeStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaNodeStatusService.class);

    @Inject
    private FreeIpaNodeStatusClientFactory freeIpaNodeStatusClientFactory;

    public CdpNodeStatuses nodeStatusReport(Stack stack, InstanceMetaData instance) throws FreeIpaClientException {
        try (CdpNodeStatusMonitorClient client = getClient(stack, instance)) {
            CdpNodeStatusRequest request = CdpNodeStatusRequest.Builder.builder()
                    .withSkipObjectMapping(true)
                    .build();
            return client.nodeStatusReport(request);
        } catch (CdpNodeStatusMonitorClientException | MalformedURLException e) {
            throw new RetryableFreeIpaClientException("Could not get node status report from stack.", e);
        }
    }

    public RPCResponse<NodeStatusProto.NodeStatusReport> nodeNetworkReport(Stack stack, InstanceMetaData instance) throws FreeIpaClientException {
        try (CdpNodeStatusMonitorClient client = getClient(stack, instance)) {
            LOGGER.debug("Fetching network report for instance: {}", instance.getInstanceId());
            return client.nodeNetworkReport();
        } catch (FreeIpaClientException e) {
            throw new RetryableFreeIpaClientException("Error during getting node network report", e);
        } catch (Exception e) {
            LOGGER.error("Getting FreeIPA node network report failed", e);
            throw new RetryableFreeIpaClientException("Getting FreeIPA node network report failed", e);
        }
    }

    private CdpNodeStatusMonitorClient getClient(Stack stack, InstanceMetaData instance) throws MalformedURLException, FreeIpaClientException {
        final Optional<String> username;
        final Optional<String> password;
        if (StringUtils.isNoneEmpty(stack.getCdpNodeStatusMonitorUser(), stack.getCdpNodeStatusMonitorPassword())) {
            username = Optional.of(stack.getCdpNodeStatusMonitorUser());
            password = Optional.of(stack.getCdpNodeStatusMonitorPassword());
        } else {
            username = Optional.empty();
            password = Optional.empty();
        }
        return freeIpaNodeStatusClientFactory.getClientWithBasicAuth(stack, instance, username, password);
    }
}
