package com.sequenceiq.cloudbreak.orchestrator.salt.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;

public class OrchestratorExceptionAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrchestratorExceptionAnalyzer.class);

    /**
     * Regex pattern for the following type of strings:
     * <code>
     * {param1=Value of param1, param2=value of param2,
     *  param2 continue in new line, param3=value of param3, ....}
     * </code>
     */
    private static final Pattern NODE_FAILURE_PATTERN = Pattern.compile("(\\w+)=(.*?)(?=,\\s*\\w+=|}|$)", Pattern.DOTALL);

    private OrchestratorExceptionAnalyzer() {
    }

    /**
     * This method parses and returns the node error parameters from the orchestratorException.getNodesWithErrors() multimap.
     * Example output: <code>node1.param1=value1, node1.param2=value2, node2.param1=value3,...</code>
     * @param orchestratorException the exception which will be analyzed
     * @return the parameters which are parsed from the orchestratorException.getNodesWithErrors() multimap.
     */
    public static Map<String, String> getNodeErrorParameters(CloudbreakOrchestratorException orchestratorException) {
        if (ExceptionUtils.getRootCause(orchestratorException) instanceof CloudbreakOrchestratorException rootCause) {
            LOGGER.error("error: {}", rootCause.getNodesWithErrors());
            return convert(rootCause.getNodesWithErrors());
        } else {
            return Map.of();
        }
    }

    private static Map<String, String> convert(Multimap<String, String> multimap) {
        return multimap.entries().stream()
                .flatMap(entry -> parseLog(entry.getValue()).entrySet().stream()
                        .map(e -> Map.entry(entry.getKey() + "." + e.getKey(), e.getValue())))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v1 + " | " + v2,
                        LinkedHashMap::new
                ));
    }

    private static Map<String, String> parseLog(String log) {
        Map<String, String> parsed = new LinkedHashMap<>();
        Matcher matcher = NODE_FAILURE_PATTERN.matcher(log);
        while (matcher.find()) {
            parsed.put(matcher.group(1).trim(), matcher.group(2).trim());
        }
        return parsed;
    }
}
