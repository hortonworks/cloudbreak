package com.sequenceiq.freeipa.service.stack;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthDetailsFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.RPCMessage;
import com.sequenceiq.freeipa.client.model.RPCResponse;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.healthagent.ApiClient;
import com.sequenceiq.freeipa.healthagent.ApiException;
import com.sequenceiq.freeipa.healthagent.JSON;
import com.sequenceiq.freeipa.healthagent.api.DefaultApi;
import com.sequenceiq.freeipa.healthagent.model.CheckEntry;
import com.sequenceiq.freeipa.healthagent.model.CheckResult;
import com.sequenceiq.freeipa.healthagent.model.ClusterCheckResult;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

@Service
public class FreeIpaHealthDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaHealthDetailsService.class);

//    private static final String EXTERNAL_COMMAND_OUTPUT = "ExternalCommandOutput";
//
//    private static final String STATUS_OK = "OK";
//
//    private static final int STATUS_GROUP = 2;
//
//    private static final int NODE_GROUP = 1;
//
//    private static final String MESSAGE_UNAVAILABLE = "Message Unavailable";
//
//    private static final Pattern RESULT_PATTERN = Pattern.compile("(ecure port|: TCP) \\([0-9]*\\): (.*)");
//
//    private static final Pattern NEW_NODE_PATTERN = Pattern.compile("Check connection from master to remote replica '(.[^\']*)");

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    private JSON json = new JSON();

    public HealthDetailsFreeIpaResponse getHealthDetails(String environmentCrn, String accountId) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(environmentCrn, accountId);
        List<InstanceMetaData> instances = stack.getAllInstanceMetaDataList();
        HealthDetailsFreeIpaResponse response = new HealthDetailsFreeIpaResponse();
        for (InstanceMetaData instance: instances) {
            if (instance.isAvailable()) {
//                try {
                    ApiClient client = new ApiClient();
                    // TODO direct VS CP mode will change the port.
                    client.setBasePath("https://" + instance.getDiscoveryFQDN() + ":5080");
                    // TODO : Replace with valid CA Cert maybe
                    client.setVerifyingSsl(false);
                    DefaultApi api = new DefaultApi(client);
                    CheckResult result;
                    try {
                        result = api.rootGet();
                        parseResults(result, response);
                    } catch (ApiException e) {
                        if (e.getCode() == HttpStatus.SC_SERVICE_UNAVAILABLE) {
                            result = json.deserialize(e.getResponseBody(), CheckResult.class);
                            parseResults(result, response);
                        } else {
                            addUnreachableResponse(instance, response, e.getLocalizedMessage());
                            LOGGER.error(String.format("Unable to check the health of FreeIPA instance: %s", instance.getInstanceId()), e);
                        }
                    }
                    // TODO: Need to look at how cluster results are put into the response.
                    ClusterCheckResult clusterCheckResult;
                    try {
                        clusterCheckResult = api.clusterGet();
                        parseClusterResults(clusterCheckResult, response);
                    } catch (ApiException e) {
                        if (e.getCode() == HttpStatus.SC_SERVICE_UNAVAILABLE) {
                            clusterCheckResult = json.deserialize(e.getResponseBody(), ClusterCheckResult.class);
                            parseClusterResults(clusterCheckResult, response);
                        } else {
                            response.setStatus(Status.UNHEALTHY);
                            LOGGER.error(String.format("Unable to check the health of FreeIPA instance: %s", instance.getInstanceId()), e);
                        }

                    }
//                    RPCResponse<Boolean> rpcResponse = checkFreeIpaHealth(stack,  instance);
//                    parseMessages(rpcResponse, response);

//                } catch (FreeIpaClientException e) {
//                }
            } else {
                NodeHealthDetails nodeResponse = new NodeHealthDetails();
                response.addNodeHealthDetailsFreeIpaResponses(nodeResponse);
                nodeResponse.setName(instance.getDiscoveryFQDN());
                nodeResponse.setStatus(instance.getInstanceStatus());
                nodeResponse.addIssue("Unable to check health as instance is " + instance.getInstanceStatus().name());
            }
        }
        return updateResponse(stack, response);
    }

    private void parseClusterResults(ClusterCheckResult result, final HealthDetailsFreeIpaResponse response) {
        switch(result.getStatus()) {
            // TODO: Possibly add DEGRADED to higher level status.
            case UNHEALTHY:
            case DEGRADED:
                response.setStatus(Status.UNHEALTHY);
                result.getReplicas().stream().forEach(replica -> replica.getChecks().stream()
                        .filter(check->check.getStatus() == CheckEntry.StatusEnum.UNHEALTHY)
                        .forEach(check->response.addIssue(check.getCheckId() + ":" + check.getDetail().values().toString())));
        }
    }

    private void parseResults(CheckResult result, HealthDetailsFreeIpaResponse response) {
        final NodeHealthDetails nodeResponse = new NodeHealthDetails();
        response.addNodeHealthDetailsFreeIpaResponses(nodeResponse);
        nodeResponse.setName(result.getHost());
        if (result.getStatus() == CheckResult.StatusEnum.HEALTHY) {
            nodeResponse.setStatus(InstanceStatus.CREATED);
        } else {
            nodeResponse.setStatus(InstanceStatus.FAILED);
            result.getChecks().stream().filter(check-> check.getStatus() == CheckEntry.StatusEnum.UNHEALTHY)
                    .forEach(check -> nodeResponse.addIssue(check.getCheckId() + ":" + check.getDetail().values().toString()));
        }
    }

    private void addUnreachableResponse(InstanceMetaData instance, HealthDetailsFreeIpaResponse response, String issue) {
        NodeHealthDetails nodeResponse = new NodeHealthDetails();
        response.addNodeHealthDetailsFreeIpaResponses(nodeResponse);
        nodeResponse.setName(instance.getDiscoveryFQDN());
        nodeResponse.setStatus(InstanceStatus.UNREACHABLE);
        nodeResponse.addIssue(issue);
    }

    private HealthDetailsFreeIpaResponse updateResponse(Stack stack, HealthDetailsFreeIpaResponse response) {
        response.setEnvironmentCrn(stack.getEnvironmentCrn());
        response.setCrn(stack.getResourceCrn());
        response.setName(stack.getName());
        if (isOverallHealthy(response)) {
            response.setStatus(DetailedStackStatus.PROVISIONED.getStatus());
        } else {
            response.setStatus(DetailedStackStatus.UNHEALTHY.getStatus());
        }
        updateResponseWithInstanceIds(response, stack);
        return response;
    }

    private void updateResponseWithInstanceIds(HealthDetailsFreeIpaResponse response, Stack stack) {
        Map<String, String> nameIdMap = getNameIdMap(stack);
        for (NodeHealthDetails node: response.getNodeHealthDetails()) {
            node.setInstanceId(nameIdMap.get(node.getName()));
        }
    }

    private Map<String, String> getNameIdMap(Stack stack) {
        return stack.getInstanceGroups().stream().flatMap(ig -> ig.getInstanceMetaData().stream())
                .collect(Collectors.toMap(InstanceMetaData::getDiscoveryFQDN, InstanceMetaData::getInstanceId));
    }

//    private RPCResponse<Boolean> checkFreeIpaHealth(Stack stack, InstanceMetaData instance) throws FreeIpaClientException {
//        FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack, instance.getDiscoveryFQDN());
//        return freeIpaClient.serverConnCheck(freeIpaClient.getHostname(), instance.getDiscoveryFQDN());
//    }

    private boolean isOverallHealthy(HealthDetailsFreeIpaResponse response) {
        for (NodeHealthDetails node: response.getNodeHealthDetails()) {
            if (node.getStatus().equals(InstanceStatus.CREATED)) {
                return true;
            }
        }
        return false;
    }

//    private void parseMessages(RPCResponse<Boolean> rpcResponse, HealthDetailsFreeIpaResponse response) {
//        String precedingMessage = MESSAGE_UNAVAILABLE;
//        NodeHealthDetails nodeResponse = null;
//        for (RPCMessage message : rpcResponse.getMessages()) {
//            Matcher nodeMatcher = NEW_NODE_PATTERN.matcher(message.getMessage());
//            if (nodeMatcher.find()) {
//                nodeResponse = new NodeHealthDetails();
//                response.addNodeHealthDetailsFreeIpaResponses(nodeResponse);
//                nodeResponse.setStatus(InstanceStatus.CREATED);
//                nodeResponse.setName(nodeMatcher.group(NODE_GROUP));
//            }
//            if (nodeResponse == null) {
//                LOGGER.info("No node for message: {}" + message.getMessage());
//            } else {
//                // When parsing the messages, if there's an error, the error
//                // appears in the preceding message.
//                if (EXTERNAL_COMMAND_OUTPUT.equals(message.getName())) {
//                    Matcher matcher = RESULT_PATTERN.matcher(message.getMessage());
//                    if (matcher.find()) {
//                        if (!STATUS_OK.equals(matcher.group(STATUS_GROUP))) {
//                            nodeResponse.setStatus(InstanceStatus.FAILED);
//                            nodeResponse.addIssue(precedingMessage);
//                        }
//                    }
//                    precedingMessage = message.getMessage();
//                }
//            }
//        }
//    }
}
