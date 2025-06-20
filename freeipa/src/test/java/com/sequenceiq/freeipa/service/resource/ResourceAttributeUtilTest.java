package com.sequenceiq.freeipa.service.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.entity.Stack;

@ExtendWith(MockitoExtension.class)
class ResourceAttributeUtilTest {

    @InjectMocks
    private ResourceAttributeUtil underTest;

    private Resource resource;

    private Stack stack;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setId(1L);

        resource = new Resource();
        resource.setId(1L);
        resource.setResourceType(ResourceType.AWS_INSTANCE);
        resource.setResourceStatus(CommonStatus.CREATED);
        resource.setResourceName("test-resource");
        resource.setStack(stack);
    }

    @Test
    void testGetTypedAttributesWithValidAttributesShouldReturnParsedObject() {
        // Given
        TestCustomAttributes customAttributes = new TestCustomAttributes("test-name", 123, true);
        resource.setAttributes(new Json(customAttributes));

        // When
        Optional<TestCustomAttributes> result = underTest.getTypedAttributes(resource, TestCustomAttributes.class);

        // Then
        assertTrue(result.isPresent());
        TestCustomAttributes parsedAttributes = result.get();
        assertEquals("test-name", parsedAttributes.getName());
        assertEquals(123, parsedAttributes.getValue());
        assertTrue(parsedAttributes.isEnabled());
    }

    @Test
    void testGetTypedAttributesWithSimpleStringAttributeShouldReturnEmptyOptional() {
        // Given
        String testValue = "test-string-value";
        resource.setAttributes(new Json(testValue));

        // When
        Optional<String> result = underTest.getTypedAttributes(resource, String.class);

        // Then
        // Simple string values don't create a map, so the method returns empty
        assertFalse(result.isPresent());
    }

    @Test
    void testGetTypedAttributesWithNullAttributesShouldThrowNullPointerException() {
        // Given
        resource.setAttributes(null);

        // When & Then
        assertThrows(NullPointerException.class,
                () -> underTest.getTypedAttributes(resource, TestCustomAttributes.class));
    }

    @Test
    void testGetTypedAttributesWithNullJsonValueShouldReturnEmptyOptional() {
        // Given
        resource.setAttributes(new Json((String) null));

        // When
        Optional<TestCustomAttributes> result = underTest.getTypedAttributes(resource, TestCustomAttributes.class);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testGetTypedAttributesWithEmptyJsonObjectShouldReturnEmptyOptional() {
        // Given
        resource.setAttributes(new Json("{}"));

        // When
        Optional<TestCustomAttributes> result = underTest.getTypedAttributes(resource, TestCustomAttributes.class);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testGetTypedAttributesWithSimpleIntegerAttributeShouldReturnEmptyOptional() {
        // Given
        Integer testValue = 42;
        resource.setAttributes(new Json(testValue));

        // When
        Optional<Integer> result = underTest.getTypedAttributes(resource, Integer.class);

        // Then
        // Simple integer values don't create a map, so the method returns empty
        assertFalse(result.isPresent());
    }

    @Test
    void testGetTypedAttributesWithInvalidJsonFormatShouldReturnEmptyOptional() {
        // Given
        resource.setAttributes(new Json("invalid-json-format"));

        // When
        Optional<VolumeSetAttributes> result = underTest.getTypedAttributes(resource, VolumeSetAttributes.class);

        // Then
        // Invalid JSON doesn't create a map, so the method returns empty
        assertFalse(result.isPresent());
    }

    @Test
    void testGetTypedAttributesWithIncompatibleTypeShouldReturnEmptyOptional() {
        // Given
        String testValue = "test-string";
        resource.setAttributes(new Json(testValue));

        // When
        Optional<VolumeSetAttributes> result = underTest.getTypedAttributes(resource, VolumeSetAttributes.class);

        // Then
        // Simple string values don't create a map, so the method returns empty
        assertFalse(result.isPresent());
    }

    @Test
    void testGetTypedAttributesWithComplexObjectAndWrongTypeShouldThrowCloudbreakServiceException() {
        // Given
        TestCustomAttributes customAttributes = new TestCustomAttributes("test", 42, true);
        resource.setAttributes(new Json(customAttributes));

        // When & Then
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.getTypedAttributes(resource, String.class));

        assertTrue(exception.getMessage().contains("Failed to parse attributes to type: String"));
    }

    @Test
    void testGetTypedAttributesWithNullResourceShouldThrowNullPointerException() {
        // When & Then
        assertThrows(NullPointerException.class,
                () -> underTest.getTypedAttributes(null, VolumeSetAttributes.class));
    }

    @Test
    void testGetTypedAttributesWithNullAttributeTypeShouldThrowIllegalArgumentException() {
        // Given
        TestCustomAttributes customAttributes = new TestCustomAttributes("test", 42, true);
        resource.setAttributes(new Json(customAttributes));

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> underTest.getTypedAttributes(resource, null));
    }

    @Test
    void testGetTypedAttributesWithCustomObjectShouldReturnParsedObject() {
        // Given
        TestCustomAttributes customAttributes = new TestCustomAttributes("test-name", 123, true);
        resource.setAttributes(new Json(customAttributes));

        // When
        Optional<TestCustomAttributes> result = underTest.getTypedAttributes(resource, TestCustomAttributes.class);

        // Then
        assertTrue(result.isPresent());
        TestCustomAttributes parsedAttributes = result.get();
        assertEquals("test-name", parsedAttributes.getName());
        assertEquals(123, parsedAttributes.getValue());
        assertTrue(parsedAttributes.isEnabled());
    }

    @Test
    void testGetTypedAttributesWithPartialCustomObjectShouldReturnParsedObjectWithDefaults() {
        // Given
        String partialJson = "{\"name\":\"partial-test\",\"value\":456}";
        resource.setAttributes(new Json(partialJson));

        // When
        Optional<TestCustomAttributes> result = underTest.getTypedAttributes(resource, TestCustomAttributes.class);

        // Then
        assertTrue(result.isPresent());
        TestCustomAttributes parsedAttributes = result.get();
        assertEquals("partial-test", parsedAttributes.getName());
        assertEquals(456, parsedAttributes.getValue());
        assertFalse(parsedAttributes.isEnabled());
    }

    @Test
    void testGetTypedAttributesWithJsonArrayForListTypeShouldReturnEmptyOptional() {
        // Given
        String jsonArray = "[\"item1\", \"item2\", \"item3\"]";
        resource.setAttributes(new Json(jsonArray));

        // When
        Optional<List> result = underTest.getTypedAttributes(resource, List.class);

        // Then
        // JSON arrays don't create a map, so the method returns empty
        assertFalse(result.isPresent());
    }

    // Test data class for testing custom object parsing
    public static class TestCustomAttributes {

        private String name;

        private int value;

        private boolean enabled;

        TestCustomAttributes() {
        }

        TestCustomAttributes(String name, int value, boolean enabled) {
            this.name = name;
            this.value = value;
            this.enabled = enabled;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}