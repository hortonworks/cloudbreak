package com.sequenceiq.mock.verification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class RequestResponseStorageService {

    private Map<String, List<Call>> calls = new HashMap<>();

    public void put(String testName, Call call) {
        List<Call> calls = this.calls.computeIfAbsent(testName, key -> new ArrayList<>());
        calls.add(call);
    }

    public List<Call> get(String testName) {
        return calls.get(testName);
    }
}
