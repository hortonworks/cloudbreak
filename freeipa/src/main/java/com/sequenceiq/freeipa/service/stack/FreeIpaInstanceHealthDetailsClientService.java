package com.sequenceiq.freeipa.service.stack;

import static java.util.function.Predicate.not;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.RPCMessage;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.RetryService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.CheckResultV1Response;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthCheckV1Response;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.PluginStatV1Response;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaHealthCheckClient;
import com.sequenceiq.freeipa.client.FreeIpaHealthCheckClientFactory;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.client.healthcheckmodel.CheckEntry;
import com.sequenceiq.freeipa.client.healthcheckmodel.CheckResult;
import com.sequenceiq.freeipa.client.healthcheckmodel.PluginStatusEntry;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.util.HealthCheckAvailabilityChecker;

@Service
public class FreeIpaInstanceHealthDetailsClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaInstanceHealthDetailsClientService.class);

    private static final InstanceStatus HEALTHY_INSTANCE_STATUS = InstanceStatus.CREATED;

    private static final String EXTERNAL_COMMAND_OUTPUT = "ExternalCommandOutput";

    private static final String STATUS_OK = "OK";

    private static final int STATUS_GROUP = 2;

    private static final String MESSAGE_UNAVAILABLE = "Message Unavailable";

    private static final Pattern RESULT_PATTERN = Pattern.compile("(ecure port|: TCP) \\([0-9]*\\): (.*)");

    private static final Pattern NEW_NODE_PATTERN = Pattern.compile("Check connection from master to remote replica '(.[^\']*)");

    private static final String HEALTHY = "HEALTHY";

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private HealthCheckAvailabilityChecker healthCheckAvailabilityChecker;

    @Inject
    private FreeIpaHealthCheckClientFactory freeIpaHealthCheckClientFactory;

    @Inject
    private RetryService retryService;

    public NodeHealthDetails getInstanceHealthDetails(Stack stack, InstanceMetaData instance) throws FreeIpaClientException {
        MDCBuilder.buildMdcContext(stack);
        if (healthCheckAvailabilityChecker.isCdpFreeIpaHeathAgentAvailable(stack)) {
            RPCResponse<CheckResult> rpcResponse = freeIpaHealthCheck(stack, instance);
            return parseMessages(rpcResponse, instance);
        } else {
            RPCResponse<Boolean> rpcResponse = legacyFreeIpaHealthCheck(stack, instance);
            return legacyParseMessages(rpcResponse, instance);
        }
    }

    public RPCResponse<Boolean> checkFreeIpaHealth(Stack stack, InstanceMetaData instance) {
        MDCBuilder.buildMdcContext(stack);
        try {
            if (healthCheckAvailabilityChecker.isCdpFreeIpaHeathAgentAvailable(stack)) {
                return toBooleanRpcResponse(freeIpaHealthCheck(stack, instance), instance.getDiscoveryFQDN());
            } else {
                return legacyFreeIpaHealthCheck(stack, instance);
            }
        } catch (RetryableFreeIpaClientException e) {
            if (instance.isNotAvailable()) {
                throw new Retry.ActionFailedNonRetryableException(ExceptionUtils.getRootCauseMessage(e), e);
            } else {
                throw new Retry.ActionFailedException(ExceptionUtils.getRootCauseMessage(e), e);
            }
        } catch (FreeIpaClientException e) {
            throw new Retry.ActionFailedNonRetryableException(ExceptionUtils.getRootCauseMessage(e), e);
        }
    }

    private RPCResponse<CheckResult> freeIpaHealthCheck(Stack stack, InstanceMetaData instance) throws FreeIpaClientException {
        if (instance.getDiscoveryFQDN() == null) {
            LOGGER.info("The health check cannot run on {} because the instance was not fully installed and it is missing the FQDN", instance);
            throw new FreeIpaClientException("The legacy health check cannot run on because the instance was not fully installed and it is missing the FQDN");
        }
        try (FreeIpaHealthCheckClient client = freeIpaHealthCheckClientFactory.getClient(stack, instance)) {
            return client.nodeHealth();
        } catch (FreeIpaClientException e) {
            throw new RetryableFreeIpaClientException("Error during healthcheck", e);
        } catch (Exception e) {
            LOGGER.error("FreeIPA health check failed", e);
            throw new RetryableFreeIpaClientException("FreeIPA health check failed", e);
        }
    }

    private RPCResponse<Boolean> legacyFreeIpaHealthCheck(Stack stack, InstanceMetaData instance) throws FreeIpaClientException {
        if (instance.getDiscoveryFQDN() == null) {
            LOGGER.info("The legacy health check cannot run on {} because the instance was not fully installed and it is missing the FQDN", instance);
            throw new FreeIpaClientException("The legacy health check cannot run on because the instance was not fully installed and it is missing the FQDN");
        }
        FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStackForLegacyHealthCheck(stack, instance.getDiscoveryFQDN());
        return freeIpaClient.serverConnCheck(freeIpaClient.getHostname(), instance.getDiscoveryFQDN());
    }

    private RPCResponse<Boolean> toBooleanRpcResponse(RPCResponse<CheckResult> nodeHealth, String discoveryFQDN) throws FreeIpaClientException {
        if (nodeHealth.getResult() == null || StringUtils.equalsIgnoreCase(discoveryFQDN, nodeHealth.getResult().getHost())) {
            RPCMessage rpcMessage = createRpcMessageFromCheckResult(nodeHealth);
            RPCResponse<Boolean> response = new RPCResponse<>();
            response.setSummary(nodeHealth.getSummary());
            response.setResult(isHealthCheckPassing(nodeHealth));
            response.setCount(nodeHealth.getCount());
            response.setTruncated(nodeHealth.getTruncated());
            response.setMessages(List.of(rpcMessage));
            response.setCompleted(nodeHealth.getCompleted());
            response.setFailed(nodeHealth.getFailed());
            response.setValue(nodeHealth.getValue());
            return response;
        } else {
            String message = String.format("Expected hostname [%s] and received [%s] are different. "
                + "This could happen when cluster proxy is used, and the instance is not registered with CP.",
                discoveryFQDN, nodeHealth.getResult().getHost());
            LOGGER.warn(message);
            throw new FreeIpaClientException(message);
        }
    }

    private RPCMessage createRpcMessageFromCheckResult(RPCResponse<CheckResult> nodeHealth) {
        List<CheckEntry> checkEntries = filterCheckEntries(nodeHealth);
        List<PluginStatusEntry> pluginStatusEntries = filterPluginStatusEntries(nodeHealth);
        CheckResult filteredCheckResult = new CheckResult();
        filteredCheckResult.setChecks(checkEntries);
        filteredCheckResult.setPluginStats(pluginStatusEntries);
        if (nodeHealth.getResult() != null) {
            filteredCheckResult.setStatus(nodeHealth.getResult().getStatus());
            filteredCheckResult.setHost(nodeHealth.getResult().getHost());
        }
        RPCMessage rpcMessage = new RPCMessage();
        rpcMessage.setName("node health check");
        rpcMessage.setMessage(JsonUtil.writeValueAsStringSilent(filteredCheckResult));
        if (nodeHealth.getFirstRpcMessage() != null) {
            rpcMessage.setCode(nodeHealth.getFirstRpcMessage().getCode());
        }
        return rpcMessage;
    }

    private List<PluginStatusEntry> filterPluginStatusEntries(RPCResponse<CheckResult> nodeHealth) {
        if (nodeHealth.getResult() == null || nodeHealth.getResult().getPluginStats() == null) {
            return List.of();
        } else {
            return nodeHealth.getResult().getPluginStats().stream()
                    .filter(not(check -> HEALTHY.equals(check.getStatus())))
                    .collect(Collectors.toList());
        }
    }

    private List<CheckEntry> filterCheckEntries(RPCResponse<CheckResult> nodeHealth) {
        if (nodeHealth.getResult() == null || nodeHealth.getResult().getChecks() == null) {
            return List.of();
        } else {
            return nodeHealth.getResult().getChecks().stream()
                    .filter(not(check -> HEALTHY.equals(check.getStatus())))
                    .collect(Collectors.toList());
        }
    }

    private NodeHealthDetails parseMessages(RPCResponse<CheckResult> rpcResponse, InstanceMetaData instanceMetaData) {
        NodeHealthDetails nodeResponse = new NodeHealthDetails();
        nodeResponse.setName(instanceMetaData.getDiscoveryFQDN());
        nodeResponse.setInstanceId(instanceMetaData.getInstanceId());
        if (isHealthCheckPassing(rpcResponse)) {
            nodeResponse.setStatus(HEALTHY_INSTANCE_STATUS);
        } else {
            nodeResponse.setStatus(InstanceStatus.UNHEALTHY);
            nodeResponse.setIssues(rpcResponse.getMessages().stream().map(RPCMessage::getMessage).collect(Collectors.toList()));
        }
        nodeResponse.setHealthChecks(getCheckResultV1Responses(rpcResponse));
        return nodeResponse;
    }

    private List<HealthCheckV1Response> getCheckResultV1Responses(RPCResponse<CheckResult> rpcResponse) {
        return rpcResponse.getMessages()
                .stream()
                .map(RPCMessage::getMessage)
                .map(e -> {
                    try {
                        CheckResult checkResult = JsonUtil.readValue(e, CheckResult.class);
                        HealthCheckV1Response healthCheckV1Response = new HealthCheckV1Response();
                        healthCheckV1Response.setHost(checkResult.getHost());
                        healthCheckV1Response.setStatus(checkResult.getStatus());
                        healthCheckV1Response.setChecks(
                                checkResult.getChecks().stream()
                                        .map(ce -> {
                                            CheckResultV1Response check = new CheckResultV1Response();
                                            check.setCheckId(ce.getCheckId());
                                            check.setStatus(ce.getStatus());
                                            check.setPlugin(ce.getPlugin());
                                            return check;
                                        })
                                        .collect(Collectors.toList())
                        );
                        healthCheckV1Response.setPluginStat(
                                checkResult.getPluginStats().stream()
                                        .map(pe -> {
                                            PluginStatV1Response pluginStatV1Response = new PluginStatV1Response();
                                            pluginStatV1Response.setPlugin(pe.getPlugin());
                                            pluginStatV1Response.setStatus(pe.getStatus());
                                            pluginStatV1Response.setResponseTime(pe.getResponseTime());
                                            pluginStatV1Response.setHost(pe.getHost());
                                            return pluginStatV1Response;
                                        })
                                        .collect(Collectors.toList())
                        );
                        return healthCheckV1Response;
                    } catch (IOException  ex) {
                        LOGGER.warn("Cannot parse health check message: {}", e, ex);
                        return new HealthCheckV1Response();
                    }
                })
                .collect(Collectors.toList());
    }

    private NodeHealthDetails legacyParseMessages(RPCResponse<Boolean> rpcResponse, InstanceMetaData instanceMetaData) {
        String precedingMessage = MESSAGE_UNAVAILABLE;
        NodeHealthDetails nodeResponse = new NodeHealthDetails();
        nodeResponse.setStatus(rpcResponse.getResult() ? InstanceStatus.CREATED : InstanceStatus.UNHEALTHY);
        nodeResponse.setName(instanceMetaData.getDiscoveryFQDN());
        nodeResponse.setInstanceId(instanceMetaData.getInstanceId());
        boolean found = false;
        for (RPCMessage message : rpcResponse.getMessages()) {
            Matcher nodeMatcher = NEW_NODE_PATTERN.matcher(message.getMessage());
            if (nodeMatcher.find()) {
                found = true;
            }
            if (!found) {
                LOGGER.info("No node for message: {}" + message.getMessage());
            } else {
                // When parsing the messages, if there's an error, the error
                // appears in the preceding message.
                if (EXTERNAL_COMMAND_OUTPUT.equals(message.getName())) {
                    Matcher matcher = RESULT_PATTERN.matcher(message.getMessage());
                    if (matcher.find()) {
                        if (!STATUS_OK.equals(matcher.group(STATUS_GROUP))) {
                            nodeResponse.addIssue(precedingMessage);
                        }
                    }
                    precedingMessage = message.getMessage();
                }
            }
        }
        return nodeResponse;
    }

    private boolean isHealthCheckPassing(RPCResponse<CheckResult> rpcResponse) {
        return rpcResponse.getMessages().stream()
                .map(RPCMessage::getCode)
                .filter(Objects::nonNull)
                .map(Response.Status.Family::familyOf)
                .map(f -> f.equals(Response.Status.Family.SUCCESSFUL))
                .reduce(Boolean::logicalAnd)
                .orElse(Boolean.FALSE);
    }
}
