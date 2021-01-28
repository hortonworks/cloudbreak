package com.sequenceiq.freeipa.service.stack;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaNodeStatusClientFactory;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.node.health.client.CdpNodeStatusMonitorClient;
import com.sequenceiq.node.health.client.model.HealthReport;

@Service
public class FreeIpaNodeStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaNodeStatusService.class);

    @Inject
    private FreeIpaNodeStatusClientFactory freeIpaNodeStatusClientFactory;

    public RPCResponse<HealthReport> nodeMeteringReport(Stack stack, InstanceMetaData instance) throws FreeIpaClientException {
        try (CdpNodeStatusMonitorClient client = freeIpaNodeStatusClientFactory.getClient(stack, instance)) {
            LOGGER.debug("Fetching metering report for instance: {}", instance.getInstanceId());
            return client.nodeMeteringReport();
        } catch (FreeIpaClientException e) {
            throw new RetryableFreeIpaClientException("Error during getting node metering report", e);
        } catch (Exception e) {
            LOGGER.error("Getting FreeIPA node metering report failed", e);
            throw new RetryableFreeIpaClientException("Getting FreeIPA node metering report failed", e);
        }
    }

    public RPCResponse<HealthReport> nodeNetworkReport(Stack stack, InstanceMetaData instance) throws FreeIpaClientException {
        try (CdpNodeStatusMonitorClient client = freeIpaNodeStatusClientFactory.getClient(stack, instance)) {
            LOGGER.debug("Fetching network report for instance: {}", instance.getInstanceId());
            return client.nodeNetworkReport();
        } catch (FreeIpaClientException e) {
            throw new RetryableFreeIpaClientException("Error during getting node network report", e);
        } catch (Exception e) {
            LOGGER.error("Getting FreeIPA node network report failed", e);
            throw new RetryableFreeIpaClientException("Getting FreeIPA node network report failed", e);
        }
    }

    public RPCResponse<HealthReport> nodeServicesReport(Stack stack, InstanceMetaData instance) throws FreeIpaClientException {
        try (CdpNodeStatusMonitorClient client = freeIpaNodeStatusClientFactory.getClient(stack, instance)) {
            LOGGER.debug("Fetching services report for instance: {}", instance.getInstanceId());
            return client.nodeServicesReport();
        } catch (FreeIpaClientException e) {
            throw new RetryableFreeIpaClientException("Error during getting node services report", e);
        } catch (Exception e) {
            LOGGER.error("Getting FreeIPA node services report failed", e);
            throw new RetryableFreeIpaClientException("Getting FreeIPA node services report failed", e);
        }
    }
}
