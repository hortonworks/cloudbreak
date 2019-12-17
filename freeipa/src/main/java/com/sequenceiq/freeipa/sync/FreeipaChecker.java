package com.sequenceiq.freeipa.sync;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.RPCResponse;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@Component
public class FreeipaChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaChecker.class);

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    private RPCResponse<Boolean> checkStatus(Stack stack) throws Exception {
        return checkedMeasure(() -> {
            FreeIpaClient freeIpaClient = checkedMeasure(() -> freeIpaClientFactory.getFreeIpaClientForStackWithPing(stack), LOGGER,
                    ":::Auto sync measure::: freeipa client is created in {}ms");
            GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
            return freeIpaClient.serverConnCheck(primaryGatewayConfig.getHostname(), primaryGatewayConfig.getHostname());
        }, LOGGER, ":::Auto sync measure::: freeipa server status is checked in {}ms");
    }

    public SyncResult getStatus(Stack stack) {
        try {
            RPCResponse<Boolean> response = checkStatus(stack);
            DetailedStackStatus status;
            String postFix = "";
            if (response.getResult()) {
                status = DetailedStackStatus.PROVISIONED;
            } else {
                status = DetailedStackStatus.UNHEALTHY;
                postFix = "Freeipa is unhealthy, ";
            }
            return new SyncResult(postFix + getMessage(response), status, response.getResult());
        } catch (HttpHostConnectException | ConnectTimeoutException e) {
            return new SyncResult("Freeipa is unreachable: " + e.getMessage(), DetailedStackStatus.UNREACHABLE, false);
        } catch (FreeIpaClientException e) {
            LOGGER.info("FreeIpaClientException occurred during status fetch: " + e.getMessage(), e);
            return new SyncResult("Freeipa is unhealthy, error occurred: " + e.getMessage(), DetailedStackStatus.UNHEALTHY, false);
        } catch (Exception e) {
            LOGGER.info("Error occurred during status fetch: " + e.getMessage(), e);
            return new SyncResult("Freeipa is unreachable, because error occurred: " + e.getMessage(), DetailedStackStatus.UNREACHABLE, false);
        }
    }

    private String getMessage(RPCResponse<Boolean> response) {
        if (response == null) {
            return "Cannot parse component status from Freeipa response, because it is null.";
        }
        return response.getMessages()
                .stream()
                .map(m -> m.getName() + ": " + m.getMessage())
                .collect(Collectors.joining(", "));
    }
}
