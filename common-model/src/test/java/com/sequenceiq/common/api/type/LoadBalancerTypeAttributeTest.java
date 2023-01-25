package com.sequenceiq.common.api.type;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class LoadBalancerTypeAttributeTest {

    @Test
    void simpleAndAttributeLoadBalancerTypesMatch() {
        assertThat(Stream.of(LoadBalancerTypeAttribute.values()).map(Enum::name).collect(Collectors.toSet()))
                .containsExactlyInAnyOrderElementsOf(Stream.of(LoadBalancerType.values()).map(Enum::name).collect(Collectors.toSet()));
    }
}
