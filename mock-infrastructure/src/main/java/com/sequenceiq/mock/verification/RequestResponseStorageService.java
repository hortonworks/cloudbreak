package com.sequenceiq.mock.verification;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class RequestResponseStorageService {

    private Map<String, List<Call>> calls = new ConcurrentHashMap<>();

    public void put(String testName, Call call) {
        List<Call> calls = this.calls.computeIfAbsent(testName, key -> new CopyOnWriteArrayList<>());
        calls.add(call);
    }

    public List<Call> get(String testName) {
        return calls.get(testName);
    }

    public List<Call> get(String testName, String path) {
        List<Call> calls = get(testName);
        if (calls == null) {
            return Collections.emptyList();
        }
        return calls.stream()
                .filter(c -> c.getUrl().contains(path))
                .collect(Collectors.toList());
    }
}
