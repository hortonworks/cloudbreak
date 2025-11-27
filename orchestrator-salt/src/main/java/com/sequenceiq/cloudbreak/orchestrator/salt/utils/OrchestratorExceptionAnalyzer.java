package com.sequenceiq.cloudbreak.orchestrator.salt.utils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;

public class OrchestratorExceptionAnalyzer {
    public static final String COMMENT = "comment";

    public static final String STDOUT = "stdout";

    public static final String STDERR = "stderr";

    public static final String NAME = "name";

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

    public static Set<HostSaltCommands> getHostSaltCommands(CloudbreakOrchestratorException orchestratorException) {
        if (ExceptionUtils.getRootCause(orchestratorException) instanceof CloudbreakOrchestratorException rootCause) {
            LOGGER.error("error: {}", rootCause.getNodesWithErrors());
            return rootCause.getNodesWithErrors().asMap().entrySet().stream()
                    .map(entry -> getHostSaltCommands(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toSet());
        } else {
            return Set.of();
        }
    }

    private static HostSaltCommands getHostSaltCommands(String host, Collection<String> commands) {
        return new HostSaltCommands(host, commands.stream().map(OrchestratorExceptionAnalyzer::getSaltCommand).toList());
    }

    private static SaltCommand getSaltCommand(String parameters) {
        Map<String, String> parameterMap = parseLog(parameters);
        if (!parameterMap.isEmpty() && parameterMap.containsKey(COMMENT)) {
            return new SaltCommand(parameterMap.get(COMMENT), parameterMap);
        } else {
            return new SaltCommand(parameters, parameterMap);
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
            String key = matcher.group(1).toLowerCase(Locale.ROOT).trim();
            String value = matcher.group(2).trim();
            if (StringUtils.isNoneBlank(key, value)) {
                parsed.put(key, value);
            }
        }
        return parsed;
    }

    public record HostSaltCommands(String host, List<SaltCommand> saltCommands) {
    }

    public record SaltCommand(String command, Map<String, String> params) {
    }
}
