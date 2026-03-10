package com.sequenceiq.cloudbreak.cm.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.cloudera.api.swagger.model.ApiConfigRecord;
import com.cloudera.api.swagger.model.ApiHostReallocateMemoryResponse;
import com.cloudera.api.swagger.model.ApiRoleConfigGroupRef;
import com.cloudera.api.swagger.model.ApiServiceRef;
import com.cloudera.api.swagger.model.AutoConfigApplicability;
import com.sequenceiq.cloudbreak.cluster.model.resetjvmparams.JvmConfigApplicability;
import com.sequenceiq.cloudbreak.cluster.model.resetjvmparams.JvmConfigRecord;
import com.sequenceiq.cloudbreak.cluster.model.resetjvmparams.ResetJvmParamsDiff;

class ApiHostReallocateMemoryResponseConverterTest {

    @Test
    void convertWhenSourceIsNull() {
        ResetJvmParamsDiff result = ApiHostReallocateMemoryResponseConverter.convert(null);

        assertNotNull(result);
        assertTrue(result.getConfigsBefore().isEmpty());
        assertTrue(result.getConfigsAfter().isEmpty());
    }

    @Test
    void convertWhenConfigsAreNull() {
        ApiHostReallocateMemoryResponse source = new ApiHostReallocateMemoryResponse();

        ResetJvmParamsDiff result = ApiHostReallocateMemoryResponseConverter.convert(source);

        assertNotNull(result);
        assertTrue(result.getConfigsBefore().isEmpty());
        assertTrue(result.getConfigsAfter().isEmpty());
    }

    @Test
    void convertWhenConfigsAreEmpty() {
        ApiHostReallocateMemoryResponse source = new ApiHostReallocateMemoryResponse()
                .configsBefore(Collections.emptyList())
                .configsAfter(Collections.emptyList());

        ResetJvmParamsDiff result = ApiHostReallocateMemoryResponseConverter.convert(source);

        assertNotNull(result);
        assertTrue(result.getConfigsBefore().isEmpty());
        assertTrue(result.getConfigsAfter().isEmpty());
    }

    @Test
    void convertMapsAllConfigFieldsCorrectly() {
        ApiRoleConfigGroupRef rcg = new ApiRoleConfigGroupRef().roleConfigGroupName("rcg-name");
        ApiServiceRef serviceRef = new ApiServiceRef().clusterName("cluster-1").serviceName("yarn");
        ApiConfigRecord recordBefore = new ApiConfigRecord()
                .name("heap_size").value("1024")
                .rcg(rcg).service(serviceRef)
                .applicability(AutoConfigApplicability.RECONFIGURABLE);
        ApiConfigRecord recordAfter = new ApiConfigRecord()
                .name("heap_size").value("2048")
                .rcg(rcg).service(serviceRef)
                .applicability(AutoConfigApplicability.RECONFIGURABLE);
        ApiHostReallocateMemoryResponse source = new ApiHostReallocateMemoryResponse()
                .configsBefore(List.of(recordBefore))
                .configsAfter(List.of(recordAfter));

        ResetJvmParamsDiff result = ApiHostReallocateMemoryResponseConverter.convert(source);

        assertEquals(1, result.getConfigsBefore().size());
        JvmConfigRecord before = result.getConfigsBefore().getFirst();
        assertEquals("heap_size", before.getName());
        assertEquals("1024", before.getValue());
        assertEquals("rcg-name", before.getRoleConfigGroupName());
        assertEquals("cluster-1", before.getClusterName());
        assertEquals("yarn", before.getServiceName());
        assertEquals(JvmConfigApplicability.RECONFIGURABLE, before.getApplicability());

        assertEquals(1, result.getConfigsAfter().size());
        JvmConfigRecord after = result.getConfigsAfter().getFirst();
        assertEquals("heap_size", after.getName());
        assertEquals("2048", after.getValue());
        assertEquals("rcg-name", after.getRoleConfigGroupName());
        assertEquals("cluster-1", after.getClusterName());
        assertEquals("yarn", after.getServiceName());
        assertEquals(JvmConfigApplicability.RECONFIGURABLE, after.getApplicability());
    }

    @Test
    void convertWhenRcgIsNullSetsRoleConfigGroupNameToNull() {
        ApiConfigRecord record = new ApiConfigRecord().name("heap_size").value("2048");
        ApiHostReallocateMemoryResponse source = new ApiHostReallocateMemoryResponse()
                .configsAfter(List.of(record));

        ResetJvmParamsDiff result = ApiHostReallocateMemoryResponseConverter.convert(source);

        assertNull(result.getConfigsAfter().getFirst().getRoleConfigGroupName());
    }

    @Test
    void convertWhenServiceRefIsNullSetsClusterAndServiceNameToNull() {
        ApiConfigRecord record = new ApiConfigRecord().name("heap_size").value("2048");
        ApiHostReallocateMemoryResponse source = new ApiHostReallocateMemoryResponse()
                .configsAfter(List.of(record));

        ResetJvmParamsDiff result = ApiHostReallocateMemoryResponseConverter.convert(source);

        assertNull(result.getConfigsAfter().getFirst().getClusterName());
        assertNull(result.getConfigsAfter().getFirst().getServiceName());
    }

    @Test
    void convertWhenApplicabilityIsNullReturnsNullApplicability() {
        ApiConfigRecord record = new ApiConfigRecord().name("heap_size").value("2048");
        ApiHostReallocateMemoryResponse source = new ApiHostReallocateMemoryResponse()
                .configsAfter(List.of(record));

        ResetJvmParamsDiff result = ApiHostReallocateMemoryResponseConverter.convert(source);

        assertNull(result.getConfigsAfter().getFirst().getApplicability());
    }

    @ParameterizedTest
    @MethodSource("applicabilityMappings")
    void convertMapsApplicabilityCorrectly(AutoConfigApplicability input, JvmConfigApplicability expected) {
        ApiConfigRecord record = new ApiConfigRecord().name("heap_size").value("2048").applicability(input);
        ApiHostReallocateMemoryResponse source = new ApiHostReallocateMemoryResponse()
                .configsAfter(List.of(record));

        ResetJvmParamsDiff result = ApiHostReallocateMemoryResponseConverter.convert(source);

        assertEquals(expected, result.getConfigsAfter().getFirst().getApplicability());
    }

    private static Stream<Arguments> applicabilityMappings() {
        return Stream.of(
                Arguments.of(AutoConfigApplicability.RECONFIGURABLE, JvmConfigApplicability.RECONFIGURABLE),
                Arguments.of(AutoConfigApplicability.UNAFFECTED_DUE_TO_EQUAL_VALUE, JvmConfigApplicability.UNAFFECTED_DUE_TO_EQUAL_VALUE),
                Arguments.of(AutoConfigApplicability.UNAFFECTED_CONFIGURED_BY_USER, JvmConfigApplicability.UNAFFECTED_CONFIGURED_BY_USER)
        );
    }
}
