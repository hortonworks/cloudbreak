package com.sequenceiq.cloudbreak.orchestrator.salt.states;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunnerInfo;

public class RunnerInfoConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunnerInfoConverter.class);

    private RunnerInfoConverter() {
    }

    static Optional<RunnerInfo> convertToRunnerInfo(String key, JsonNode runnerInfoNode) {
        if (!runnerInfoNode.isObject()) {
            LOGGER.trace("Cannot convert JSON to runner info Object as the value is not "
                            + "a JSON object {} for key '{}'", runnerInfoNode, key);
            return Optional.empty();
        }
        RunnerInfo runnerInfo = new RunnerInfo();
        runnerInfo.setStateId(key);
        runnerInfo.setChanges(getChanges(runnerInfoNode));
        runnerInfo.setComment(runnerInfoNode.get("comment").asText());
        double duration = getDuration(runnerInfoNode);
        runnerInfo.setDuration(duration);
        if (runnerInfoNode.has("name")) {
            runnerInfo.setName(runnerInfoNode.get("name").asText());
        }
        runnerInfo.setResult(runnerInfoNode.get("result").asBoolean());
        String runNum = runnerInfoNode.get("__run_num__").asText();
        runnerInfo.setRunNum(Integer.parseInt(runNum));
        runnerInfo.setStartTime(runnerInfoNode.get("start_time").asText());
        return Optional.of(runnerInfo);
    }

    private static Map<String, Object> getChanges(JsonNode runnerInfoNode) {
        Map<String, Object> changesMap = new HashMap<>();
        if (runnerInfoNode.has("changes")) {
            StreamSupport.stream(Spliterators.spliteratorUnknownSize(runnerInfoNode.get("changes").fields(), 0), false)
                    .forEach(changeEntry -> changesMap.put(changeEntry.getKey(), changeEntry.getValue().asText()));
        }
        return changesMap;
    }

    private static double getDuration(JsonNode runnerInfoNode) {
        double duration;
        try {
            String[] durationArray = String.valueOf(runnerInfoNode.get("duration").asText()).split(" ");
            duration = Double.parseDouble(durationArray[0]);
        } catch (NumberFormatException ignored) {
            duration = 0.0;
        }
        return duration;
    }
}
