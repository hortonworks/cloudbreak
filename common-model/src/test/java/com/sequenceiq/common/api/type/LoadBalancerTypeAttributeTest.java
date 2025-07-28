package com.sequenceiq.common.api.type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class LoadBalancerTypeAttributeTest {

    @Test
    void simpleAndAttributeLoadBalancerTypesMatch() {
        assertThat(Stream.of(LoadBalancerTypeAttribute.values()).map(Enum::name).collect(Collectors.toSet()))
                .containsExactlyInAnyOrderElementsOf(Stream.of(LoadBalancerType.values()).map(Enum::name).collect(Collectors.toSet()));
    }

    @Test
    void testFromMapWithValidName() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "PUBLIC");
        LoadBalancerTypeAttribute result = LoadBalancerTypeAttribute.fromMap(map);
        assertEquals(LoadBalancerTypeAttribute.PUBLIC, result);
    }

    @Test
    void testFromMapWithInvalidName() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "INVALID");
        assertThrows(IllegalArgumentException.class, () -> LoadBalancerTypeAttribute.fromMap(map));
    }

    @Test
    void testFromMapWithMissingName() {
        Map<String, Object> map = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> LoadBalancerTypeAttribute.fromMap(map));
    }

    @Test
    void testFromMapWithNonStringName() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", 123);
        assertThrows(IllegalArgumentException.class, () -> LoadBalancerTypeAttribute.fromMap(map));
    }
}
