package com.sequenceiq.mock.verification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class RequestResponseStorageService {

    private Map<String, List<Call>> calls = new HashMap<>();

    private Set<String> disabledCallStorage = new HashSet<>();

    public void put(String mockUuid, Call call) {
        if (isEnabledToStore(mockUuid)) {
            List<Call> calls = this.calls.computeIfAbsent(mockUuid, key -> new ArrayList<>());
            calls.add(call);
        }
    }

    public List<Call> get(String mockUuid) {
        return calls.get(mockUuid);
    }

    public void disableCallStorage(String mockUuid) {
        disabledCallStorage.add(mockUuid);
    }

    public void enableCallStorage(String mockUuid) {
        disabledCallStorage.remove(mockUuid);
    }

    public boolean isEnabledToStore(String mockUuid) {
        return !disabledCallStorage.contains(mockUuid);
    }

    public void cleanup(String mockUuid) {
        calls.remove(mockUuid);
        disabledCallStorage.remove(mockUuid);
    }
}
