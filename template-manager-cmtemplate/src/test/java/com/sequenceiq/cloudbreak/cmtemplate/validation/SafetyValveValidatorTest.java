package com.sequenceiq.cloudbreak.cmtemplate.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.template.model.SafetyValve;

public class SafetyValveValidatorTest {

    private final SafetyValveValidator safetyValveValidator = new SafetyValveValidator();

    @Test
    void testValidateWithNullValue() {
        SafetyValve safetyValve = new SafetyValve("dummyservice", "dummyrole", "testParam", null);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> safetyValveValidator.validate(safetyValve));
        assertEquals(exception.getMessage(), "Safety valve value is not present for {serviceType: dummyservice, " +
                "roleType: dummyrole, safety valve: testParam}");
    }

    @ParameterizedTest(name = "Validate for Non Xml format Safety Valve {0}")
    @MethodSource("provideArguments")
    void testValidateForNonXmlFormatSafetyValve(String paramName) {
        SafetyValve safetyValve = new SafetyValve("dummyservice", "dummyrole", paramName, "dummyValue");
        assertDoesNotThrow(() -> safetyValveValidator.validate(safetyValve));
    }

    @Test
    void testValidateWithValidValue() {
        SafetyValve safetyValve = new SafetyValve("dummyservice", "dummyrole", "testParam",
                "<property><name>mapreduce.fileoutputcommitter.algorithm.version</name><value>1</value></property>" +
                        "<property><name>mapreduce.input.fileinputformat.list-status.num-threads</name><value>100</value></property>");
        assertDoesNotThrow(() -> safetyValveValidator.validate(safetyValve));
    }

    @Test
    void testValidateWithInValidValue() {
        SafetyValve safetyValve = new SafetyValve("dummyservice", "dummyrole", "testParam",
                "<property><name>mapreduce.fileoutputcommitter.algorithm.version</name><value>1</value></property>" +
                        "<name>mapreduce.input.fileinputformat.list-status.num-threads</name><value>100</value></property>");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> safetyValveValidator.validate(safetyValve));
        assertEquals(exception.getMessage(), "Format is invalid for {serviceType: dummyservice, roleType: dummyrole, " +
                "safety valve: testParam, raw value: <property><name>mapreduce.fileoutputcommitter.algorithm.version</name><value>1</value>" +
                "</property><name>mapreduce.input.fileinputformat.list-status.num-threads</name><value>100</value></property>}");
    }

    private static Stream<Arguments> provideArguments() throws IOException {
        return List.of("hue_service_safety_valve",
                "spark-conf/spark-defaults.conf_client_config_safety_valve",
                "impala_cmd_args_safety_valve",
                "hdfs_client_env_safety_valve",
                "mapreduce_client_env_safety_valve",
                "spark3-conf/spark-defaults.conf_client_config_safety_valve",
                "REGIONSERVER_role_env_safety_valve",
                "hue_server_hue_safety_valve").stream()
                .map(param -> Arguments.of(param));
    }
}
