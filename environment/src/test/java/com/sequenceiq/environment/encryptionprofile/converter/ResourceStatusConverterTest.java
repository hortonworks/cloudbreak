package com.sequenceiq.environment.encryptionprofile.converter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;

class ResourceStatusConverterTest {

    private ResourceStatusConverter converter;

    @BeforeEach
    void setUp() {
        converter = new ResourceStatusConverter();
    }

    @Test
    void convertToDatabaseColumnReturnEnumName() {
        String result = converter.convertToDatabaseColumn(ResourceStatus.USER_MANAGED);
        assertThat(result).isEqualTo(ResourceStatus.USER_MANAGED.name());
    }

    @Test
    void convertToDatabaseColumnReturnDefaultWhenNull() {
        String result = converter.convertToDatabaseColumn(null);
        assertThat(result).isEqualTo(ResourceStatus.USER_MANAGED.name());
    }

    @Test
    void convertToEntityAttributeReturnEnumValue() {
        ResourceStatus status = converter.convertToEntityAttribute("USER_MANAGED");
        assertThat(status).isEqualTo(ResourceStatus.USER_MANAGED);
    }

    @Test
    void convertToEntityAttributeReturnDefaultForUnknownValue() {
        ResourceStatus status = converter.convertToEntityAttribute("UNKNOWN_VALUE");
        assertThat(status).isEqualTo(ResourceStatus.USER_MANAGED);
    }

    @Test
    void convertToEntityAttributeReturnDefaultForNull() {
        ResourceStatus status = converter.convertToEntityAttribute(null);
        assertThat(status).isEqualTo(ResourceStatus.USER_MANAGED);
    }
}