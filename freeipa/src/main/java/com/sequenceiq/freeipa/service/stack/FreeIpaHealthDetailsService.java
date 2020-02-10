package com.sequenceiq.freeipa.service.stack;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthDetailsFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.RPCMessage;
import com.sequenceiq.freeipa.client.model.RPCResponse;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

@Service
public class FreeIpaHealthDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaHealthDetailsService.class);

    private static final String EXTERNAL_COMMAND_OUTPUT = "ExternalCommandOutput";

    private static final String STATUS_OK = "OK";

    private static final int STATUS_GROUP = 2;

    private static final int NODE_GROUP = 1;

    private static final String MESSAGE_UNAVAILABLE = "Message Unavailable";

    private static final Pattern RESULT_PATTERN = Pattern.compile("(ecure port|: TCP) \\([0-9]*\\): (.*)");

    private static final Pattern NEW_NODE_PATTERN = Pattern.compile("Check connection from master to remote replica '(.[^\']*)");

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    public HealthDetailsFreeIpaResponse getHealthDetails(String environmentCrn, String accountId) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(environmentCrn, accountId);
        InstanceMetaData master = findMaster(stack);
        Optional<RPCResponse<Boolean>> rpcResponse = Optional.empty();
        try {
            rpcResponse = Optional.ofNullable(checkFreeIpaHealth(stack, master));
        } catch (FreeIpaClientException e) {
            LOGGER.error("Unable to check the health of FreeIPA.", e);
        }
        return createResponse(stack, rpcResponse, master);
    }

    private HealthDetailsFreeIpaResponse createResponse(Stack stack, Optional<RPCResponse<Boolean>> rpcResponse, InstanceMetaData master) {
        HealthDetailsFreeIpaResponse response = new HealthDetailsFreeIpaResponse();
        response.setEnvironmentCrn(stack.getEnvironmentCrn());
        response.setCrn(stack.getResourceCrn());
        if (rpcResponse.isPresent()) {
            response.setName((String) rpcResponse.get().getValue());
            parseMessages(rpcResponse.get(), response);
            if (isOverallHealthy(response)) {
                response.setStatus(DetailedStackStatus.PROVISIONED.getStatus());
            } else {
                response.setStatus(DetailedStackStatus.UNHEALTHY.getStatus());
            }
        } else {
            response.setStatus(DetailedStackStatus.UNREACHABLE.getStatus());
            NodeHealthDetails nodeResponse = new NodeHealthDetails();
            response.addNodeHealthDetailsFreeIpaResponses(nodeResponse);
            nodeResponse.setName(master.getDiscoveryFQDN());
            nodeResponse.setStatus(Status.UNREACHABLE);
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

    private InstanceMetaData findMaster(Stack stack) {
        InstanceGroup masterGroup = stack.getInstanceGroups().stream()
                .filter(instanceGroup -> InstanceGroupType.MASTER == instanceGroup.getInstanceGroupType()).findFirst().get();
        return masterGroup.getNotDeletedInstanceMetaDataSet().stream().findFirst().get();
    }

    private RPCResponse<Boolean> checkFreeIpaHealth(Stack stack, InstanceMetaData master) throws FreeIpaClientException {
        FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
        return freeIpaClient.serverConnCheck(master.getDiscoveryFQDN(), master.getDiscoveryFQDN());
    }

    private boolean isOverallHealthy(HealthDetailsFreeIpaResponse response) {
        for (NodeHealthDetails node: response.getNodeHealthDetails()) {
            if (node.getStatus().equals(Status.AVAILABLE)) {
                return true;
            }
        }
        return false;
    }

    private void parseMessages(RPCResponse<Boolean> rpcResponse, HealthDetailsFreeIpaResponse response) {
        String precedingMessage = MESSAGE_UNAVAILABLE;
        NodeHealthDetails nodeResponse = null;
        for (RPCMessage message : rpcResponse.getMessages()) {
            Matcher nodeMatcher = NEW_NODE_PATTERN.matcher(message.getMessage());
            if (nodeMatcher.find()) {
                nodeResponse = new NodeHealthDetails();
                response.addNodeHealthDetailsFreeIpaResponses(nodeResponse);
                nodeResponse.setStatus(Status.AVAILABLE);
                nodeResponse.setName(nodeMatcher.group(NODE_GROUP));
            }
            if (nodeResponse == null) {
                LOGGER.info("No node for message: {}" + message.getMessage());
            } else {
                // When parsing the messages, if there's an error, the error
                // appears in the preceding message.
                if (EXTERNAL_COMMAND_OUTPUT.equals(message.getName())) {
                    Matcher matcher = RESULT_PATTERN.matcher(message.getMessage());
                    if (matcher.find()) {
                        if (!STATUS_OK.equals(matcher.group(STATUS_GROUP))) {
                            nodeResponse.setStatus(Status.UNHEALTHY);
                            nodeResponse.addIssue(precedingMessage);
                        }
                    }
                    precedingMessage = message.getMessage();
                }
            }
        }
    }
}
