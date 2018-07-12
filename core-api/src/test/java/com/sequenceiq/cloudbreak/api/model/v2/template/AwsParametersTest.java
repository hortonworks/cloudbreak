package com.sequenceiq.cloudbreak.api.model.v2.template;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class AwsParametersTest {

    @Test
    public void testSpotPriceWhenSet() {
        AwsTemplateParameters underTest = new AwsTemplateParameters();
        underTest.setSpotPrice(21.23D);

        Map<String, Object> actual = underTest.asMap();

        Assert.assertEquals(21.23, actual.get("spotPrice"));
    }

    @Test
    public void testSpotPriceWhenUnset() {
        AwsTemplateParameters underTest = new AwsTemplateParameters();

        Map<String, Object> actual = underTest.asMap();

        Assert.assertNull(actual.get("spotPrice"));
    }

    @Test
    public void testEncryption() {
        AwsTemplateParameters underTest = new AwsTemplateParameters();
        Encryption encryption = new Encryption();
        encryption.setKey("someKey");
        encryption.setType("CUSTOM");
        underTest.setEncryption(encryption);
        underTest.setEncrypted(true);

        Map<String, Object> actual = underTest.asMap();

        Assert.assertEquals("someKey", actual.get("key"));
        Assert.assertEquals(EncryptionType.CUSTOM, actual.get("type"));
        Assert.assertEquals(true, actual.get("encrypted"));
        Assert.assertEquals(TemplatePlatformType.AWS, actual.get(BaseTemplateParameter.PLATFORM_TYPE));
    }

    @Test
    public void testEncryptionWhenNull() {
        AwsTemplateParameters underTest = new AwsTemplateParameters();

        Map<String, Object> actual = underTest.asMap();

        Assert.assertNull(actual.get("key"));
        Assert.assertNull(actual.get("type"));
        Assert.assertNull(actual.get("encrypted"));
    }
}
