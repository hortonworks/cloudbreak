package com.sequenceiq.cloudbreak.orchestrator.salt.states;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunnerInfoObject;

public class JidInfoResponseTransformer {

    private JidInfoResponseTransformer() {
    }

    public static Map<String, Map<String, RunnerInfoObject>> getHighStates(Map map) {
        Map<String, List<Map<String, Map<String, Map<String, Map<String, Object>>>>>> tmp = map;
        Map<String, Map<String, Map<String, Object>>> stringMapMap = tmp.get("return").get(0).get("data");
        Map<String, Map<String, RunnerInfoObject>> result = new HashMap<>();

        for (Map.Entry<String, Map<String, Map<String, Object>>> stringMapEntry : stringMapMap.entrySet()) {
            result.put(stringMapEntry.getKey(), runnerInfoObjects(stringMapEntry.getValue()));
        }

        return result;
    }

    public static Map<String, Map<String, RunnerInfoObject>> getSimpleStates(Map map) {
        Map<String, List<Map<String, Map<String, Map<String, Object>>>>> tmp = map;
        Map<String, Map<String, Map<String, Object>>> stringMapMap = tmp.get("return").get(0);

        Map<String, Map<String, RunnerInfoObject>> result = new HashMap<>();

        for (Map.Entry<String, Map<String, Map<String, Object>>> stringMapEntry : stringMapMap.entrySet()) {
            result.put(stringMapEntry.getKey(), runnerInfoObjects(stringMapEntry.getValue()));
        }

        return result;
    }

    private static Map<String, RunnerInfoObject> runnerInfoObjects(Map<String, Map<String, Object>> map) {
        Map<String, RunnerInfoObject> runnerInfoObjectMap = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> stringMapEntry : map.entrySet()) {
            Map<String, Object> value = stringMapEntry.getValue();
            RunnerInfoObject runnerInfoObject = new RunnerInfoObject();
            Object changes = value.get("changes");
            runnerInfoObject.setChanges(changes == null ? Collections.emptyMap() : (Map<String, Object>) changes);
            runnerInfoObject.setComment(String.valueOf(value.get("comment")));
            runnerInfoObject.setDuration(String.valueOf(value.get("duration")));
            runnerInfoObject.setName(String.valueOf(value.get("name")));
            runnerInfoObject.setResult(Boolean.valueOf(String.valueOf(value.get("result"))));
            String runNum = String.valueOf(value.get("__run_num__"));
            runnerInfoObject.setRunNum(runNum == null ? -1 : Integer.parseInt(runNum));
            runnerInfoObject.setStartTime(String.valueOf(value.get("start_time")));
            runnerInfoObjectMap.put(stringMapEntry.getKey(), runnerInfoObject);
        }
        return runnerInfoObjectMap;
    }
}
