package com.sequenceiq.cloudbreak.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;

import org.hibernate.validator.HibernateValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class CustomConfigurationPropertyParametersTest {

    private LocalValidatorFactoryBean localValidatorFactory;

    @BeforeEach
    public void setUp() {
        localValidatorFactory = new LocalValidatorFactoryBean();
        localValidatorFactory.setProviderClass(HibernateValidator.class);
        localValidatorFactory.afterPropertiesSet();
    }

    private CustomConfigurationPropertyParameters createValid() {
        return new CustomConfigurationPropertyParameters("dfs.replication", "3", "DATANODE", "HDFS");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "dfs.replication",
            "mapreduce.map.memory.mb",
            "yarn.nodemanager.resource.memory-mb",
            "fs.defaultFS",
            "hive.metastore.warehouse.dir",
            "a",
            "A1"
    })
    void testPropertyNameValid(String name) {
        CustomConfigurationPropertyParameters params = createValid();
        params.setName(name);
        Set<ConstraintViolation<CustomConfigurationPropertyParameters>> violations = localValidatorFactory.validate(params);
        long nameViolations = violations.stream().filter(v -> v.getPropertyPath().toString().equals("name")).count();
        assertEquals(0, nameViolations, "Expected no violations for name: " + name);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1startsWithDigit",
            ".starts.with.dot",
            "-starts-with-hyphen",
            "has spaces",
            "has<angle>brackets",
            "has;semicolon",
            "has/slash",
            "name with special!chars",
            "<script>alert(1)</script>"
    })
    void testPropertyNameInvalid(String name) {
        CustomConfigurationPropertyParameters params = createValid();
        params.setName(name);
        Set<ConstraintViolation<CustomConfigurationPropertyParameters>> violations = localValidatorFactory.validate(params);
        long nameViolations = violations.stream().filter(v -> v.getPropertyPath().toString().equals("name")).count();
        assertTrue(nameViolations > 0, "Expected violation for name: " + name);
    }

    @Test
    void testPropertyNameTooLong() {
        CustomConfigurationPropertyParameters params = createValid();
        params.setName("a".repeat(257));
        Set<ConstraintViolation<CustomConfigurationPropertyParameters>> violations = localValidatorFactory.validate(params);
        long nameViolations = violations.stream().filter(v -> v.getPropertyPath().toString().equals("name")).count();
        assertTrue(nameViolations > 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "3",
            "1024m",
            "true",
            "/tmp/hive",
            "hdfs://nameservice:8020/path",
            "org.apache.hadoop.io.compress.SnappyCodec",
            "host1:8080,host2:8080",
            "",
            "value with spaces and numbers 123",
            "<property><name>yarn.scheduler.capacity.root.queues</name><value>default</value></property>",
            "<configuration><property><name>key</name><value>val</value></property></configuration>",
            "value<with>brackets"
    })
    void testPropertyValueValid(String value) {
        CustomConfigurationPropertyParameters params = createValid();
        params.setValue(value);
        Set<ConstraintViolation<CustomConfigurationPropertyParameters>> violations = localValidatorFactory.validate(params);
        long valueViolations = violations.stream().filter(v -> v.getPropertyPath().toString().equals("value")).count();
        assertEquals(0, valueViolations, "Expected no violations for value: " + value);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert(1)</script>"
    })
    void testPropertyValueWithXmlContentIsValid(String value) {
        CustomConfigurationPropertyParameters params = createValid();
        params.setValue(value);
        Set<ConstraintViolation<CustomConfigurationPropertyParameters>> violations = localValidatorFactory.validate(params);
        long valueViolations = violations.stream().filter(v -> v.getPropertyPath().toString().equals("value")).count();
        assertEquals(0, valueViolations, "Angle brackets should be allowed in value for safety valve XML configs: " + value);
    }

    @Test
    void testPropertyValueWithControlCharsInvalid() {
        CustomConfigurationPropertyParameters params = createValid();
        params.setValue("value\u0000with\u0001control");
        Set<ConstraintViolation<CustomConfigurationPropertyParameters>> violations = localValidatorFactory.validate(params);
        long valueViolations = violations.stream().filter(v -> v.getPropertyPath().toString().equals("value")).count();
        assertTrue(valueViolations > 0, "Expected violation for value containing control characters");
    }

    @Test
    void testPropertyValueTooLong() {
        CustomConfigurationPropertyParameters params = createValid();
        params.setValue("x".repeat(4097));
        Set<ConstraintViolation<CustomConfigurationPropertyParameters>> violations = localValidatorFactory.validate(params);
        long valueViolations = violations.stream().filter(v -> v.getPropertyPath().toString().equals("value")).count();
        assertTrue(valueViolations > 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "HDFS",
            "CORE_SETTINGS",
            "STREAMS_MESSAGING_MANAGER",
            "KAFKA",
            "HIVE_ON_TEZ",
            "A1"
    })
    void testServiceTypeValid(String serviceType) {
        CustomConfigurationPropertyParameters params = createValid();
        params.setServiceType(serviceType);
        Set<ConstraintViolation<CustomConfigurationPropertyParameters>> violations = localValidatorFactory.validate(params);
        long serviceTypeViolations = violations.stream().filter(v -> v.getPropertyPath().toString().equals("serviceType")).count();
        assertEquals(0, serviceTypeViolations, "Expected no violations for serviceType: " + serviceType);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "hdfs",
            "Hdfs",
            "1INVALID",
            "HAS SPACE",
            "HAS-HYPHEN",
            "HAS.DOT",
            "<script>"
    })
    void testServiceTypeInvalid(String serviceType) {
        CustomConfigurationPropertyParameters params = createValid();
        params.setServiceType(serviceType);
        Set<ConstraintViolation<CustomConfigurationPropertyParameters>> violations = localValidatorFactory.validate(params);
        long serviceTypeViolations = violations.stream().filter(v -> v.getPropertyPath().toString().equals("serviceType")).count();
        assertTrue(serviceTypeViolations > 0, "Expected violation for serviceType: " + serviceType);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "NAMENODE",
            "DATANODE",
            "KAFKA_BROKER",
            "REGIONSERVER",
            "HIVESERVER2"
    })
    void testRoleTypeValid(String roleType) {
        CustomConfigurationPropertyParameters params = createValid();
        params.setRoleType(roleType);
        Set<ConstraintViolation<CustomConfigurationPropertyParameters>> violations = localValidatorFactory.validate(params);
        long roleTypeViolations = violations.stream().filter(v -> v.getPropertyPath().toString().equals("roleType")).count();
        assertEquals(0, roleTypeViolations, "Expected no violations for roleType: " + roleType);
    }

    @ParameterizedTest
    @NullSource
    void testRoleTypeNullIsValid(String roleType) {
        CustomConfigurationPropertyParameters params = createValid();
        params.setRoleType(roleType);
        Set<ConstraintViolation<CustomConfigurationPropertyParameters>> violations = localValidatorFactory.validate(params);
        long roleTypeViolations = violations.stream().filter(v -> v.getPropertyPath().toString().equals("roleType")).count();
        assertEquals(0, roleTypeViolations, "Null roleType should be valid");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "namenode",
            "NameNode",
            "1INVALID",
            "HAS SPACE",
            "<script>alert</script>"
    })
    void testRoleTypeInvalid(String roleType) {
        CustomConfigurationPropertyParameters params = createValid();
        params.setRoleType(roleType);
        Set<ConstraintViolation<CustomConfigurationPropertyParameters>> violations = localValidatorFactory.validate(params);
        long roleTypeViolations = violations.stream().filter(v -> v.getPropertyPath().toString().equals("roleType")).count();
        assertTrue(roleTypeViolations > 0, "Expected violation for roleType: " + roleType);
    }

    @Test
    void testFullyValidObject() {
        CustomConfigurationPropertyParameters params = createValid();
        Set<ConstraintViolation<CustomConfigurationPropertyParameters>> violations = localValidatorFactory.validate(params);
        assertEquals(0, violations.size(), "Valid object should have no violations but got: " +
                violations.stream().map(v -> v.getPropertyPath() + ": " + v.getMessage()).collect(Collectors.joining(", ")));
    }

    @Test
    void testNullFieldsProduceViolations() {
        CustomConfigurationPropertyParameters params = new CustomConfigurationPropertyParameters();
        Set<ConstraintViolation<CustomConfigurationPropertyParameters>> violations = localValidatorFactory.validate(params);
        assertTrue(violations.size() >= 3, "Null name, value, and serviceType should all produce violations");
    }
}
