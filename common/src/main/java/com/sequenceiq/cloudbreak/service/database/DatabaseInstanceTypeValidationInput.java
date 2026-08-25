package com.sequenceiq.cloudbreak.service.database;

import java.util.Map;

import com.sequenceiq.common.model.Architecture;

public record DatabaseInstanceTypeValidationInput(
        String regionName,
        String requestedInstanceType,
        String defaultInstanceType,
        Architecture desiredArchitecture,
        Map<String, InstanceTypeSpecs> availableTypes) {

    public record InstanceTypeSpecs(Integer cpu, Float memoryInGb, Architecture architecture) {

        public static InstanceTypeSpecs fromProperties(Map<String, Object> properties) {
            Integer cpu = parseInteger(properties.get("Cpu"));
            Float memory = parseFloat(properties.get("Memory"));
            Architecture arch = Architecture.fromStringWithFallback(properties.get("Architecture"));
            return new InstanceTypeSpecs(cpu, memory, arch);
        }

        static Integer parseInteger(Object value) {
            if (value == null) {
                return null;
            }
            if (value instanceof Number number) {
                return number.intValue();
            }
            try {
                return Integer.valueOf(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        static Float parseFloat(Object value) {
            if (value == null) {
                return null;
            }
            if (value instanceof Number number) {
                return number.floatValue();
            }
            try {
                return Float.valueOf(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
