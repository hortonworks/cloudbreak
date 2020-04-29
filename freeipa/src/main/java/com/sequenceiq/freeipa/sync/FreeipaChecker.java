package com.sequenceiq.freeipa.sync;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.FreeIpaHostNotAvailableException;
import com.sequenceiq.freeipa.client.model.RPCResponse;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@Component
public class FreeipaChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaChecker.class);

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    private List<RPCResponse<Boolean>> checkStatus(Stack stack, Set<InstanceMetaData> checkableInstances) throws Exception {
        return checkedMeasure(() -> {
            List<RPCResponse<Boolean>> statuses = new LinkedList<>();
            for (InstanceMetaData instanceMetaData : checkableInstances) {
                String hostname = instanceMetaData.getDiscoveryFQDN();
                FreeIpaClient freeIpaClient = checkedMeasure(() -> freeIpaClientFactory.getFreeIpaClientForStackWithPing(stack, hostname), LOGGER,
                        ":::Auto sync::: freeipa client is created in {}ms");
                statuses.add(checkedMeasure(() -> freeIpaClient.serverConnCheck(freeIpaClient.getHostname(), hostname), LOGGER,
                        ":::Auto sync::: freeipa server_conncheck ran in {}ms"));
            }
            return statuses;
        }, LOGGER, ":::Auto sync::: freeipa server status is checked in {}ms");
    }

    public SyncResult getStatus(Stack stack, Set<InstanceMetaData> checkableInstances) {
        try {
            Set<InstanceMetaData> notTermiatedStackInstances = instanceMetaDataService.findNotTerminatedForStack(stack.getId());
            List<RPCResponse<Boolean>> responses = checkStatus(stack, checkableInstances);
            DetailedStackStatus status;
            String postFix = "";
            Boolean result = !responses.isEmpty() && responses.stream().allMatch(RPCResponse::getResult);
            if (result && responses.size() == notTermiatedStackInstances.size()) {
                status = DetailedStackStatus.PROVISIONED;
            } else {
                status = DetailedStackStatus.UNHEALTHY;
                postFix = "Freeipa is unhealthy, ";
            }
            return new SyncResult(postFix + getMessages(responses), status, result);
        } catch (FreeIpaClientException e) {
            LOGGER.info("FreeIpaClientException occurred during status fetch: " + e.getMessage(), e);
            Throwable t = FreeIpaClientExceptionUtil.getAncestorCauseBeforeFreeIpaClientExceptions(e);
            if (t instanceof FreeIpaHostNotAvailableException) {
                return new SyncResult("Freeipa is unreachable: " + t.getMessage(), DetailedStackStatus.UNREACHABLE, false);
            }
            return new SyncResult("Freeipa is unhealthy, error occurred: " + e.getMessage(), DetailedStackStatus.UNHEALTHY, false);
        } catch (Exception e) {
            LOGGER.info("Error occurred during status fetch: " + e.getMessage(), e);
            return new SyncResult("Freeipa is unreachable, because error occurred: " + e.getMessage(), DetailedStackStatus.UNREACHABLE, false);
        }
    }

    private String getMessages(List<RPCResponse<Boolean>> responses) {
        return responses.stream()
                .map(RPCResponse::getMessages)
                .flatMap(List::stream)
                .map(m -> m.getName() + ": " + m.getMessage())
                .collect(Collectors.joining(", "));
    }

    private String getPrimaryHostname(Set<InstanceMetaData> checkableInstances) {
        InstanceMetaData instanceMetaData = checkableInstances.stream()
                .filter(im -> InstanceMetadataType.GATEWAY_PRIMARY.equals(im.getInstanceMetadataType()))
                .findFirst().orElseThrow(() -> new NotFoundException("Gateway instance does not found"));
        return instanceMetaData.getDiscoveryFQDN();
    }
}
