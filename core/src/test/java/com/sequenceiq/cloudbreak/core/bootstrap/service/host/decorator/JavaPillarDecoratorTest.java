package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.MapAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;

@ExtendWith(MockitoExtension.class)
class JavaPillarDecoratorTest {

    private static final int JAVA_VERSION = 11;

    private JavaPillarDecorator underTest;

    @Mock
    private StackDto stackDto;

    @Mock
    private Stack stack;

    private Map<String, SaltPillarProperties> servicePillar;

    @BeforeEach
    void setUp() {
        underTest = new JavaPillarDecorator();
        ReflectionTestUtils.setField(underTest, "cryptoComplyPath", "ccj-path");
        ReflectionTestUtils.setField(underTest, "cryptoComplyHash", "ccj-hash");
        ReflectionTestUtils.setField(underTest, "bouncyCastleTlsPath", "bctls-path");
        ReflectionTestUtils.setField(underTest, "bouncyCastleTlsHash", "bctls-hash");

        when(stackDto.isOnGovPlatformVariant()).thenReturn(true);
        when(stackDto.getStack()).thenReturn(stack);
        when(stack.getJavaVersion()).thenReturn(JAVA_VERSION);
        servicePillar = new HashMap<>();
    }

    @Test
    void noJavaVersion() {
        when(stack.getJavaVersion()).thenReturn(null);

        underTest.decorateWithJavaProperties(stackDto, servicePillar);

        assertThatJavaProperties().doesNotContainKey("version");
    }

    @Test
    void javaVersion() {
        underTest.decorateWithJavaProperties(stackDto, servicePillar);

        assertThatJavaProperties().containsEntry("version", JAVA_VERSION);
    }

    @ParameterizedTest
    @ValueSource(strings = {"cryptoComplyPath", "cryptoComplyHash", "bouncyCastleTlsPath", "bouncyCastleTlsHash"})
    void missingSafeLogicPropertyForNonGovStack(String property) {
        when(stackDto.isOnGovPlatformVariant()).thenReturn(false);
        ReflectionTestUtils.setField(underTest, property, null);

        assertThatCode(() -> underTest.decorateWithJavaProperties(stackDto, servicePillar)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"cryptoComplyPath", "cryptoComplyHash", "bouncyCastleTlsPath", "bouncyCastleTlsHash"})
    void missingSafeLogicPropertyForGovStack(String property) {
        ReflectionTestUtils.setField(underTest, property, null);

        assertThatThrownBy(() -> underTest.decorateWithJavaProperties(stackDto, servicePillar))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Required SafeLogic property is blank for application: " + property);
    }

    @Test
    void noSafeLogicPropertiesForNonGovStack() {
        when(stackDto.isOnGovPlatformVariant()).thenReturn(false);

        underTest.decorateWithJavaProperties(stackDto, servicePillar);

        assertThatJavaProperties().doesNotContainKey("safelogic");
    }

    @Test
    void safeLogicPropertiesForGovStack() {
        underTest.decorateWithJavaProperties(stackDto, servicePillar);

        assertThatSafeLogicProperties()
                .containsEntry("cryptoComplyPath", "ccj-path")
                .containsEntry("cryptoComplyHash", "ccj-hash")
                .containsEntry("bouncyCastleTlsPath", "bctls-path")
                .containsEntry("bouncyCastleTlsHash", "bctls-hash");
    }

    private MapAssert<String, Object> assertThatJavaProperties() {
        return assertThat((Map<String, Object>) servicePillar.get("java").getProperties().get("java"));
    }

    private MapAssert<String, Object> assertThatSafeLogicProperties() {
        Map<String, Object> javaProperties = (Map<String, Object>) servicePillar.get("java").getProperties().get("java");
        return assertThat((Map<String, Object>) javaProperties.get("safelogic"));
    }

}
