package com.sequenceiq.freeipa.telemetry;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.telemetry.VmLogsService;
import com.sequenceiq.common.api.telemetry.model.VmLog;

class FreeipaVmLogsServiceTest {
    private static VmLogsService underTest;

    @BeforeAll
    static void beforeAll() {
        underTest = new VmLogsService();
        underTest.init();
    }

    @Test
    void validateUniquePathEntriesInVmLogsJson() {
        List<VmLog> logList = underTest.getVmLogs();
        List<String> duplicatePathEntries = logList.stream()
                .collect(Collectors.groupingBy(o -> o.getPath(), Collectors.counting()))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        assertTrue(duplicatePathEntries.isEmpty(),
                "Non unique path entry in vm-logs.json found. Duplicate path entries are: %s".formatted(duplicatePathEntries));
    }

    @Test
    void validateUniqueLabelEntriesInVmLogsJson() {
        List<VmLog> logList = underTest.getVmLogs();
        List<String> duplicateLabelEntries = logList.stream()
                .collect(Collectors.groupingBy(o -> o.getLabel(), Collectors.counting()))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        assertTrue(duplicateLabelEntries.isEmpty(), "Found duplicate labels in vm-logs.json. Duplicate labels are: %s".formatted(duplicateLabelEntries));
    }
}