package com.sequenceiq.cloudbreak.converter.v2.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.model.v2.template.EncryptionType;
import com.sequenceiq.cloudbreak.api.model.v2.template.GcpParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.KeyEncryptionMethod;

public class ParametersToGcpTemplateParametersConverterTest {

    @InjectMocks
    private ParametersToGcpTemplateParametersConverter underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void convertWithoutEncryption() {
        Map<String, Object> source = new HashMap<>();

        GcpParameters convert = underTest.convert(source);

        assertNotNull(convert.getEncryption());
        assertEquals(EncryptionType.DEFAULT.name(), convert.getEncryption().getType());
    }

    @Test
    public void convertWithEncryption() {
        Map<String, Object> source = new HashMap<>();
        source.put("type", EncryptionType.CUSTOM);
        source.put("key", "someKey");
        source.put("keyEncryptionMethod", "RSA");
        GcpParameters convert = underTest.convert(source);

        assertNotNull(convert.getEncryption());
        assertEquals(EncryptionType.CUSTOM.name(), convert.getEncryption().getType());
        assertEquals(KeyEncryptionMethod.RSA.name(), convert.getEncryption().getKeyEncryptionMethod());
        assertNull(convert.getEncryption().getKey());
    }

    @Test(expected = IllegalArgumentException.class)
    public void convertWithInvalidEncryptionType() {
        Map<String, Object> source = new HashMap<>();
        source.put("type", "INVALID");
        source.put("key", "someKey");
        source.put("keyEncryptionMethod", "RSA");
        underTest.convert(source);
    }

    @Test(expected = IllegalArgumentException.class)
    public void convertWithInvalidEncryptionMethod() {
        Map<String, Object> source = new HashMap<>();
        source.put("type", "CUSTOM");
        source.put("key", "someKey");
        source.put("keyEncryptionMethod", "NSA");
        underTest.convert(source);
    }
}