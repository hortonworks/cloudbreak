package com.sequenceiq.cloudbreak.converter.v2.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.model.v2.template.GcpEncryptionType;
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
        assertEquals(GcpEncryptionType.DEFAULT.name(), convert.getEncryption().getType());
    }

    @Test
    public void convertWithEncryption() {
        Map<String, Object> source = new HashMap<>();
        source.put("type", GcpEncryptionType.CUSTOM);
        source.put("key", "someKey");
        source.put("keyEncryptionMethod", "RSA");
        GcpParameters convert = underTest.convert(source);

        assertNotNull(convert.getEncryption());
        assertEquals(GcpEncryptionType.CUSTOM.name(), convert.getEncryption().getType());
        assertEquals(KeyEncryptionMethod.RSA.name(), convert.getEncryption().getKeyEncryptionMethod());
        assertEquals("someKey", convert.getEncryption().getKey());
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