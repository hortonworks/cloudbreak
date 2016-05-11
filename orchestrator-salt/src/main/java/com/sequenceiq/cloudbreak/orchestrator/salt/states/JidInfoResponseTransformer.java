package com.sequenceiq.cloudbreak.orchestrator.salt.states;

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
            RunnerInfoObject runnerInfoObject = new RunnerInfoObject();
            runnerInfoObject.setChanges((Map<String, Object>) stringMapEntry.getValue().get("changes"));
            runnerInfoObject.setComment(stringMapEntry.getValue().get("comment").toString());
            runnerInfoObject.setDuration(stringMapEntry.getValue().get("duration").toString());
            runnerInfoObject.setName(stringMapEntry.getValue().get("name").toString());
            runnerInfoObject.setResult(stringMapEntry.getValue().get("result").toString());
            runnerInfoObject.setRunNum(stringMapEntry.getValue().get("__run_num__").toString());
            runnerInfoObject.setStartTime(stringMapEntry.getValue().get("start_time").toString());
            runnerInfoObjectMap.put(stringMapEntry.getKey(), runnerInfoObject);
        }
        return runnerInfoObjectMap;
    }
}
