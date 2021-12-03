package com.sequenceiq.mock.verification;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class RequestResponseStorageService {
    private static final Logger LOGGER = getLogger(RequestResponseStorageService.class);

    private final Map<String, List<Call>> calls = new ConcurrentHashMap<>();

    private final Set<String> enabledCallStorage = new HashSet<>();

    public void put(String mockUuid, Call call) {
        if (isEnabledToStore(mockUuid)) {
            List<Call> calls = getCalls(mockUuid).orElse(new CopyOnWriteArrayList<>());
            calls.add(call);
            this.calls.put(mockUuid, calls);
        } else {
            LOGGER.debug("Call store is disabled for {}, {}", mockUuid, call.getUri());
        }
    }

    public ResponseEntity<?> get(String mockUuid, String path) {
        Optional<List<Call>> calls = getCalls(mockUuid);
        if (!isEnabledToStore(mockUuid) && (calls.isEmpty() || CollectionUtils.isEmpty(calls.get()))) {
            String message = "Cannot fetch the calls, because this function is disabled for the resource: " + mockUuid;
            LOGGER.error(message);
            return ResponseEntity.badRequest().body(message);
        }
        if (calls.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        List<Call> filteredCalls = calls.get().stream()
                .filter(c -> c.getUrl().contains(path))
                .collect(Collectors.toList());
        return ResponseEntity.ok(filteredCalls);
    }

    private Optional<List<Call>> getCalls(String mockUuid) {
        return calls.entrySet().stream()
                .filter(e -> isEnabledToStore(mockUuid))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    public void disableCallStorage(String mockUuid) {
        LOGGER.info("Disable the call storage of verification for {}", mockUuid);
        enabledCallStorage.remove(mockUuid);
    }

    public void enableCallStorage(String mockUuid) {
        LOGGER.info("Enable the call storage of verification for {}", mockUuid);
        enabledCallStorage.add(mockUuid);
    }

    public boolean isEnabledToStore(String mockUuid) {
        return enabledCallStorage.stream().anyMatch(mockUuid::contains);
    }

}
