package com.sequenceiq.cloudbreak.converter.v2.template;


import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.model.v2.template.AwsParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.BaseTemplateParameter;
import com.sequenceiq.cloudbreak.api.model.v2.template.Encryption;
import com.sequenceiq.cloudbreak.api.model.v2.template.EncryptionType;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;

public class AwsTemplateParametersToParametersConverterTest {

    @InjectMocks
    private AwsTemplateParametersToParametersConverter underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSpotPriceWhenSet() {
        AwsParameters awsParameters = new AwsParameters();
        awsParameters.setSpotPrice(21.23D);

        Map<String, Object> actual = underTest.convert(awsParameters);

        Assert.assertEquals(21.23, actual.get("spotPrice"));
    }

    @Test
    public void testSpotPriceWhenUnset() {
        AwsParameters awsParameters = new AwsParameters();

        Map<String, Object> actual = underTest.convert(awsParameters);

        Assert.assertNull(actual.get("spotPrice"));
    }

    @Test
    public void testEncryption() {
        AwsParameters awsParameters = new AwsParameters();
        Encryption encryption = new Encryption();
        encryption.setKey("someKey");
        encryption.setType("CUSTOM");
        awsParameters.setEncryption(encryption);
        awsParameters.setEncrypted(true);

        Map<String, Object> actual = underTest.convert(awsParameters);

        Assert.assertEquals("someKey", actual.get("key"));
        Assert.assertEquals(EncryptionType.CUSTOM, actual.get("type"));
        Assert.assertEquals(true, actual.get("encrypted"));
        Assert.assertEquals(CloudConstants.AWS, actual.get(BaseTemplateParameter.PLATFORM_TYPE));
    }

    @Test
    public void testEncryptionWhenNull() {
        AwsParameters awsParameters = new AwsParameters();

        Map<String, Object> actual = underTest.convert(awsParameters);

        Assert.assertNull(actual.get("key"));
        Assert.assertEquals(EncryptionType.NONE, actual.get("type"));
        Assert.assertEquals(false, actual.get("encrypted"));
    }

}