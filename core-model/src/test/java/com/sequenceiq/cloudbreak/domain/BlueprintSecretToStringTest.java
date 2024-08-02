package com.sequenceiq.cloudbreak.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.service.secret.domain.Secret;

class BlueprintSecretToStringTest {

    private BlueprintSecretToString underTest = new BlueprintSecretToString();

    @Test
    void testConvertToDatabaseColumnWhenEmptySecret() {
        String databaseColumn = underTest.convertToDatabaseColumn(new Secret(null, null));
        assertEquals(BlueprintSecretToString.BLANK, databaseColumn);
    }

    @Test
    void testConvertToDatabaseColumnWhenValidSecret() {
        String databaseColumn = underTest.convertToDatabaseColumn(new Secret(null, "secret"));
        assertEquals("secret", databaseColumn);
    }

    @Test
    void testConvertToEntityAttributeWhenBlankDbData() {
        Secret entityAttribute = underTest.convertToEntityAttribute(BlueprintSecretToString.BLANK);
        assertNull(entityAttribute.getSecret());
        assertNull(entityAttribute.getRaw());
    }

    @Test
    void testConvertToEntityAttributeWhenValidDbData() {
        Secret entityAttribute = underTest.convertToEntityAttribute("entity");
        assertEquals("entity", entityAttribute.getSecret());
    }

}