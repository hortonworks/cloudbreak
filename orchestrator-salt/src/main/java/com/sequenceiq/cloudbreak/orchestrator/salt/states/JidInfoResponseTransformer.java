package com.sequenceiq.cloudbreak.orchestrator.salt.states;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.orchestrator.salt.domain.RunnerInfo;

public class JidInfoResponseTransformer {

    private JidInfoResponseTransformer() {
    }

    public static Map<String, List<RunnerInfo>> getHighStates(Map map) {
        Map<String, Object> stringObjectMap = ((Map<String, List<Map<String, Map<String, Object>>>>) map).get("return").get(0).get("data");
        Map<String, List<RunnerInfo>> result = new HashMap<>();
        for (Map.Entry<String, Object> stringObjectEntry : stringObjectMap.entrySet()) {
            if (stringObjectEntry.getValue() instanceof Map) {
                Map<String, Map<String, Object>> mapValue = (Map<String, Map<String, Object>>) stringObjectEntry.getValue();
                result.put(stringObjectEntry.getKey(), runnerInfoObjects(mapValue));
            } else if (stringObjectEntry.getValue() instanceof List) {
                List<String> listValue = (List<String>) stringObjectEntry.getValue();
                if (!listValue.isEmpty()) {
                    throw new RuntimeException("Salt execution went wrong: " + listValue.get(0));
                }
            } else {
                throw new UnsupportedOperationException("Not supported Salt response: " + stringObjectEntry.getValue().getClass());
            }
        }

        return result;
    }

    public static Map<String, List<RunnerInfo>> getSimpleStates(Map map) {
        Map<String, Map<String, Map<String, Object>>> stringMapMap =
                ((Map<String, List<Map<String, Map<String, Map<String, Object>>>>>) map).get("return").get(0);
        Map<String, List<RunnerInfo>> result = new HashMap<>();
        for (Map.Entry<String, Map<String, Map<String, Object>>> stringMapEntry : stringMapMap.entrySet()) {
            result.put(stringMapEntry.getKey(), runnerInfoObjects(stringMapEntry.getValue()));
        }

        return result;
    }

    private static List<RunnerInfo> runnerInfoObjects(Map<String, Map<String, Object>> map) {
        List<RunnerInfo> runnerInfoList = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> stringMapEntry : map.entrySet()) {
            Map<String, Object> value = stringMapEntry.getValue();
            RunnerInfo runnerInfo = new RunnerInfo();
            runnerInfo.setStateId(stringMapEntry.getKey());
            Object changes = value.get("changes");
            runnerInfo.setChanges(changes == null ? Collections.emptyMap() : (Map<String, Object>) changes);
            runnerInfo.setComment(String.valueOf(value.get("comment")));
            double duration;
            try {
                String[] durationArray = String.valueOf(value.get("duration")).split(" ");
                duration = Double.parseDouble(durationArray[0]);
            } catch (NumberFormatException nfe) {
                duration = 0.0;
            }
            runnerInfo.setDuration(duration);
            runnerInfo.setName(String.valueOf(value.get("name")));
            runnerInfo.setResult(Boolean.valueOf(String.valueOf(value.get("result"))));
            String runNum = String.valueOf(value.get("__run_num__"));
            runnerInfo.setRunNum(runNum == null ? -1 : Integer.parseInt(runNum));
            runnerInfo.setStartTime(String.valueOf(value.get("start_time")));
            runnerInfoList.add(runnerInfo);
        }
        Collections.sort(runnerInfoList, new RunnerInfo.RunNumComparator());
        return runnerInfoList;
    }
}
