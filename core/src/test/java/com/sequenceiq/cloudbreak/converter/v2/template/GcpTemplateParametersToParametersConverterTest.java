package com.sequenceiq.cloudbreak.converter.v2.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.model.v2.template.BaseTemplateParameter;
import com.sequenceiq.cloudbreak.api.model.v2.template.GcpEncryption;
import com.sequenceiq.cloudbreak.api.model.v2.template.GcpEncryptionType;
import com.sequenceiq.cloudbreak.api.model.v2.template.GcpParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.KeyEncryptionMethod;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;

public class GcpTemplateParametersToParametersConverterTest {

    @InjectMocks
    private GcpTemplateParametersToParametersConverter underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testEncryption() {
        GcpParameters awsParameters = new GcpParameters();
        GcpEncryption encryption = new GcpEncryption();
        encryption.setKey("someKey");
        encryption.setType("CUSTOM");
        encryption.setKeyEncryptionMethod("RSA");
        awsParameters.setEncryption(encryption);

        Map<String, Object> actual = underTest.convert(awsParameters);

        assertEquals("someKey", actual.get("key"));
        assertEquals(GcpEncryptionType.CUSTOM, actual.get("type"));
        assertEquals(KeyEncryptionMethod.RSA, actual.get("keyEncryptionMethod"));
        assertEquals(CloudConstants.GCP, actual.get(BaseTemplateParameter.PLATFORM_TYPE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidEncryptionType() {
        GcpParameters awsParameters = new GcpParameters();
        GcpEncryption encryption = new GcpEncryption();
        encryption.setKey("someKey");
        encryption.setType("INVALID");
        encryption.setKeyEncryptionMethod("RSA");
        awsParameters.setEncryption(encryption);

        underTest.convert(awsParameters);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidEncryptionMethod() {
        GcpParameters awsParameters = new GcpParameters();
        GcpEncryption encryption = new GcpEncryption();
        encryption.setKey("someKey");
        encryption.setType("CUSTOM");
        encryption.setKeyEncryptionMethod("NSA");
        awsParameters.setEncryption(encryption);

        underTest.convert(awsParameters);
    }

    @Test
    public void testEncryptionWhenNull() {
        GcpParameters awsParameters = new GcpParameters();

        Map<String, Object> actual = underTest.convert(awsParameters);

        assertNull(actual.get("key"));
        assertEquals(GcpEncryptionType.DEFAULT, actual.get("type"));
    }
}