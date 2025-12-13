package com.sequenceiq.environment.credential.validation.definition;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.environment.credential.service.ResourceDefinitionService;
import com.sequenceiq.environment.exception.MissingParameterException;

@ExtendWith(SpringExtension.class)
public class CredentialDefinitionServiceTest {
    public static final Json JSON_SELECTOR = new Json("{\"alma\":\"barack\",\"selector\":\"what\"}");

    public static final Json JSON = new Json("{\"alma\":\"barack\"}");

    public static final String PLATFORM = "platform";

    public static final Definition DEFINITION2;

    public static final Definition DEFINITION_MANDATORY2;

    public static final Definition DEFINITION_SENSITIVE2;

    public static final Definition DEFINITION_SELECTOR_EMPTY2;

    public static final Definition DEFINITION_SELECTOR2;

    public static final Definition DEFINITION_SELECTOR_FOR2;

    public static final Value TEST_VALUE_OPTIONAL;

    public static final Value TEST_VALUE2_OPTIONAL;

    public static final Value TEST_VALUE_SENSITIVE;

    public static final Value TEST_VALUE_MANDATORY;

    public static final Selector TEST_SELECTOR;

    public static final Selector TEST_SELECTOR_FOR;

    static {
        TEST_VALUE_OPTIONAL = new Value();
        TEST_VALUE_OPTIONAL.setName("alma");
        TEST_VALUE_OPTIONAL.setType("type");
        TEST_VALUE_OPTIONAL.setOptional(true);
        TEST_VALUE_OPTIONAL.setSensitive(false);

        TEST_VALUE2_OPTIONAL = new Value();
        TEST_VALUE2_OPTIONAL.setName("what");
        TEST_VALUE2_OPTIONAL.setType("type");
        TEST_VALUE2_OPTIONAL.setOptional(true);
        TEST_VALUE2_OPTIONAL.setSensitive(false);

        TEST_VALUE_MANDATORY = new Value();
        TEST_VALUE_MANDATORY.setName("szilva");
        TEST_VALUE_MANDATORY.setType("type");
        TEST_VALUE_MANDATORY.setOptional(false);
        TEST_VALUE_MANDATORY.setSensitive(false);

        TEST_VALUE_SENSITIVE = new Value();
        TEST_VALUE_SENSITIVE.setName("szilva");
        TEST_VALUE_SENSITIVE.setType("type");
        TEST_VALUE_SENSITIVE.setOptional(false);
        TEST_VALUE_SENSITIVE.setSensitive(true);

        TEST_SELECTOR = new Selector();
        TEST_SELECTOR.setName("what");
        TEST_SELECTOR.setParent("szulo");
        TEST_SELECTOR.setValues(List.of());

        TEST_SELECTOR_FOR = new Selector();
        TEST_SELECTOR_FOR.setName("what");
        TEST_SELECTOR_FOR.setParent("szulo");
        TEST_SELECTOR_FOR.setValues(List.of(TEST_VALUE_OPTIONAL));

        DEFINITION2 = new Definition();
        DEFINITION2.setSelectors(List.of());
        DEFINITION2.setValues(List.of(TEST_VALUE_OPTIONAL));

        DEFINITION_MANDATORY2 = new Definition();
        DEFINITION_MANDATORY2.setSelectors(List.of());
        DEFINITION_MANDATORY2.setValues(List.of(TEST_VALUE_OPTIONAL, TEST_VALUE_MANDATORY));

        DEFINITION_SENSITIVE2 = new Definition();
        DEFINITION_SENSITIVE2.setSelectors(List.of());
        DEFINITION_SENSITIVE2.setValues(List.of(TEST_VALUE_OPTIONAL, TEST_VALUE_SENSITIVE));

        DEFINITION_SELECTOR_EMPTY2 = new Definition();
        DEFINITION_SELECTOR_EMPTY2.setSelectors(List.of());
        DEFINITION_SELECTOR_EMPTY2.setValues(List.of(TEST_VALUE_OPTIONAL, TEST_VALUE2_OPTIONAL));

        DEFINITION_SELECTOR2 = new Definition();
        DEFINITION_SELECTOR2.setSelectors(List.of(TEST_SELECTOR));
        DEFINITION_SELECTOR2.setValues(List.of(TEST_VALUE_OPTIONAL, TEST_VALUE2_OPTIONAL));

        DEFINITION_SELECTOR_FOR2 = new Definition();
        DEFINITION_SELECTOR_FOR2.setSelectors(List.of(TEST_SELECTOR_FOR));
        DEFINITION_SELECTOR_FOR2.setValues(List.of(TEST_VALUE_OPTIONAL, TEST_VALUE2_OPTIONAL));
    }

    @MockBean
    private ResourceDefinitionService resourceDefinitionService;

    @Inject
    private CredentialDefinitionService credentialDefinitionServiceUnderTest;

    @Test
    public void testCredentialDefinitionServiceIsNotNull() {
        assertNotNull(credentialDefinitionServiceUnderTest);
    }

    @Test
    public void testCheckProperties() throws JsonProcessingException {
        when(resourceDefinitionService.getResourceDefinition(anyString(), anyString())).thenReturn(JsonUtil.writeValueAsString(DEFINITION2));
        credentialDefinitionServiceUnderTest.checkPropertiesRemoveSensitives(Platform.platform(PLATFORM), JSON);
    }

    @Test
    public void testInvalidJson() throws JsonProcessingException {
        when(resourceDefinitionService.getResourceDefinition(anyString(), anyString())).thenReturn(JsonUtil.writeValueAsString(DEFINITION2));
        Json invalidJson = new Json("nothing is here");
        assertDoesNotThrow(executeCheckProperties(invalidJson));
    }

    @Test
    public void testMandatoryIsMissing() throws JsonProcessingException {
        when(resourceDefinitionService.getResourceDefinition(anyString(), anyString())).thenReturn(JsonUtil.writeValueAsString(DEFINITION_MANDATORY2));
        assertThrows(MissingParameterException.class, executeCheckProperties(JSON));
    }

    @Test
    public void testOptionalIsMissing() throws JsonProcessingException {
        when(resourceDefinitionService.getResourceDefinition(anyString(), anyString())).thenReturn(JsonUtil.writeValueAsString(DEFINITION2));
        assertDoesNotThrow(executeCheckProperties(JSON));
        assertEquals(1, JSON.getMap().size());
    }

    @Test
    public void testSensitiveRemoved() throws JsonProcessingException {
        when(resourceDefinitionService.getResourceDefinition(anyString(), anyString())).thenReturn(JsonUtil.writeValueAsString(DEFINITION_SENSITIVE2));
        Json json = new Json("{\"alma\":\"barack\",\"szilva\":\"sensitivevalue\"}");
        assertDoesNotThrow(executeCheckProperties(json));
        assertEquals(1, json.getMap().size());
    }

    @Test
    public void testNotSpecifiedRemoved() throws JsonProcessingException {
        when(resourceDefinitionService.getResourceDefinition(anyString(), anyString())).thenReturn(JsonUtil.writeValueAsString(DEFINITION2));
        Json json = new Json("{\"alma\":\"barack\",\"szilva\":\"notinthedefinition\"}");
        assertDoesNotThrow(executeCheckProperties(json));
        assertEquals(1, json.getMap().size());
    }

    @Test
    public void testSelectorEmpty() throws JsonProcessingException {
        when(resourceDefinitionService.getResourceDefinition(anyString(), anyString())).thenReturn(JsonUtil.writeValueAsString(DEFINITION_SELECTOR_EMPTY2));
        assertDoesNotThrow(executeCheckProperties(JSON_SELECTOR));
        assertEquals(1, JSON_SELECTOR.getMap().size());
    }

    @Test
    public void testSelector() throws JsonProcessingException {
        when(resourceDefinitionService.getResourceDefinition(anyString(), anyString())).thenReturn(JsonUtil.writeValueAsString(DEFINITION_SELECTOR2));
        assertDoesNotThrow(executeCheckProperties(JSON_SELECTOR));
        assertEquals(1, JSON_SELECTOR.getMap().size());
    }

    @Test
    public void testSelectorLeave() throws JsonProcessingException {
        when(resourceDefinitionService.getResourceDefinition(anyString(), anyString())).thenReturn(JsonUtil.writeValueAsString(DEFINITION_SELECTOR_FOR2));
        assertDoesNotThrow(executeCheckProperties(JSON_SELECTOR));
        assertEquals(1, JSON_SELECTOR.getMap().size());
    }

    private Executable executeCheckProperties(Json invalidJson) {
        return () ->
                credentialDefinitionServiceUnderTest.checkPropertiesRemoveSensitives(Platform.platform(PLATFORM), invalidJson);
    }

    @Configuration
    @Import(CredentialDefinitionService.class)
    static class Config {
    }

}
