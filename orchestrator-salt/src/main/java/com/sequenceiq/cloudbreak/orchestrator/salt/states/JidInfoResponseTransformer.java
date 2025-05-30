package com.sequenceiq.cloudbreak.orchestrator.salt.states;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.JidInfoResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunnerInfo;

public class JidInfoResponseTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JidInfoResponseTransformer.class);

    private static final String UNRESPONSIVE_MINION_MSG = "Minion did not return";

    private JidInfoResponseTransformer() {
    }

    public static Map<String, List<RunnerInfo>> getHighStates(JidInfoResponse jidInfoResponse) {
        return getStateResponse(jidInfoResponse, true);
    }

    public static Map<String, List<RunnerInfo>> getSimpleStates(JidInfoResponse jidInfoResponse) {
        return getStateResponse(jidInfoResponse, false);
    }

    private static Map<String, List<RunnerInfo>> getStateResponse(JidInfoResponse jidInfoResponse, boolean highstate) {
        if (!jidInfoResponse.isEmpty()) {
            if (highstate) {
                LOGGER.debug("Validate jid info high state response before conversion: {}", jidInfoResponse);
                validateHighstateResponse(jidInfoResponse);
            } else {
                LOGGER.debug("Converting from state response: {}", jidInfoResponse);
            }
            return fillRunnerInfoFromHostsResponse(jidInfoResponse.getResults(), highstate);
        }
        return new HashMap<>();
    }

    private static Map<String, List<RunnerInfo>> fillRunnerInfoFromHostsResponse(
            JsonNode hostResults, boolean highstate) {
        Map<String, List<RunnerInfo>> result = new HashMap<>();
        if (hostResults != null) {
            List<String> unresponsiveMinions = getUnresponsiveMinions(hostResults);
            iteratorToFiniteStream(hostResults.fieldNames()).forEach(
                    host -> convertAndAddRunnerInfoByHost(hostResults, highstate, result, host));
            if (CollectionUtils.isNotEmpty(unresponsiveMinions) && highstate) {
                Multimap<String, Map<String, String>> nodesWithErrors = ArrayListMultimap.create();
                result.forEach((hostname, runnerInfos) -> {
                    if (runnerInfos != null) {
                        runnerInfos.stream()
                                .filter(Predicate.not(RunnerInfo::getResult))
                                .forEach(runnerInfo -> {
                                    nodesWithErrors.put(hostname, runnerInfo.getErrorResultSummary());
                                });
                    }
                });
                String unresponsiveMinionsListStr = StringUtils.join(unresponsiveMinions, ",");
                LOGGER.debug("Found unresponsive minions: {}", unresponsiveMinionsListStr);
                throw new SaltExecutionWentWrongException(String.format("The following salt minions have been unresponsive during salt highstate: %s",
                        unresponsiveMinionsListStr), unresponsiveMinions, nodesWithErrors);
            }
        }
        return result;
    }

    private static List<String> getUnresponsiveMinions(JsonNode hostResults) {
        return iteratorToFiniteStream(hostResults.fields())
                .filter(nodeEntry -> nodeEntry != null && nodeEntry.getValue().isTextual() && nodeEntry.getValue().asText().equals(UNRESPONSIVE_MINION_MSG))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private static void convertAndAddRunnerInfoByHost(JsonNode hostResults, boolean highstate,
            Map<String, List<RunnerInfo>> result, String hostName) {
        try {
            List<RunnerInfo> runnerInfos = runnerInfoObjects(hostResults, hostName);
            if (runnerInfos.isEmpty() && !highstate) {
                LOGGER.trace("Fill runner info with empty value for {}", hostName);
                result.put(hostName, null);
            } else {
                LOGGER.debug("Converting runner info for {}", hostName);
                result.put(hostName, runnerInfos);
            }
        } catch (RuntimeException e) {
            LOGGER.debug("Catching runtime error during converting runner info object", e);
            if (highstate) {
                throw e;
            } else {
                result.put(hostName, null);
            }
        }
    }

    private static List<RunnerInfo> runnerInfoObjects(JsonNode hostResults, String hostName) {
        return iteratorToFiniteStream(hostResults.get(hostName).fields())
                .map(jsonNode -> RunnerInfoConverter.convertToRunnerInfo(jsonNode.getKey(), jsonNode.getValue()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(new RunnerInfo.RunNumComparator())
                .collect(Collectors.toList());
    }

    private static void validateHighstateResponse(JidInfoResponse jidInfoResponse) {
        jidInfoResponse.getReturn()
                .forEach(node -> {
                            validateOnHighstateResults(jidInfoResponse, node);
                        }
                );
    }

    private static void validateOnHighstateResults(JidInfoResponse jidInfoResponse, JsonNode node) {
        iteratorToFiniteStream(node.fields())
                .forEach(fieldEntry -> {
                            validateHighstateResultEntry(jidInfoResponse, node, fieldEntry);
                        }
                );
    }

    private static void validateHighstateResultEntry(JidInfoResponse jidInfoResponse, JsonNode returnNode,
            Map.Entry<String, JsonNode> fieldEntry) {
        String fieldName = fieldEntry.getKey();
        JsonNode returnFieldNode = fieldEntry.getValue();
        if (!jidInfoResponse.hasDataFieldInResult() && isInvalidObject(returnFieldNode)) {
            throw new UnsupportedOperationException("Not supported Salt highstate response type: "
                    + returnFieldNode.getNodeType()
                    + ", response part: " + returnFieldNode);
        } else if (returnFieldNode.isArray()) {
            errorOnNonEmptyListResponse(returnNode, fieldName, returnFieldNode);
        } else if (returnFieldNode.isObject()) {
            errorOnHostListResponse(returnNode, returnFieldNode);
        }
    }

    private static void errorOnNonEmptyListResponse(JsonNode node, String fieldName, JsonNode returnFieldNode) {
        if (returnFieldNode.size() != 0) {
            String errorMessage = StreamSupport.stream(
                            node.withArray(fieldName).spliterator(), false)
                    .map(JsonNode::asText)
                    .reduce((s, s2) -> s + "; " + s2).get();
            throw new SaltExecutionWentWrongException("Salt execution went wrong: " + errorMessage);
        }
    }

    private static void errorOnHostListResponse(JsonNode node, JsonNode returnFieldNode) {
        Iterator<String> internalFieldName = returnFieldNode.fieldNames();
        while (internalFieldName.hasNext()) {
            String internalFieldNameStr = internalFieldName.next();
            JsonNode internalJsonField = returnFieldNode.get(internalFieldNameStr);
            if (internalJsonField.isArray()) {
                throw new SaltExecutionWentWrongException("Salt execution went wrong:: " + internalJsonField);
            }
            if (internalJsonField.isTextual()) {
                LOGGER.debug("Found textual salt minion response for '{}' node: {}", internalFieldNameStr, internalJsonField);
            }
        }
    }

    private static boolean isInvalidObject(JsonNode returnFieldNode) {
        return returnFieldNode.isNull() || returnFieldNode.isTextual() || returnFieldNode.isNumber();
    }

    private static <T> Stream<T> iteratorToFiniteStream(final Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
    }
}
