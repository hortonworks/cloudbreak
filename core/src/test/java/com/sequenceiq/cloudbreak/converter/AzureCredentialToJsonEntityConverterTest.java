package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.CredentialResponse;
import com.sequenceiq.cloudbreak.controller.validation.RequiredAzureCredentialParam;
import com.sequenceiq.cloudbreak.domain.AzureCredential;

public class AzureCredentialToJsonEntityConverterTest extends AbstractEntityConverterTest<AzureCredential> {

    private AzureCredentialToJsonConverter underTest;

    @Before
    public void setUp() {
        underTest = new AzureCredentialToJsonConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        CredentialResponse result = underTest.convert(getSource());
        // THEN
        assertEquals("subscription-id", result.getParameters().get(RequiredAzureCredentialParam.SUBSCRIPTION_ID.getName()));
    }

    @Override
    public AzureCredential createSource() {
        return (AzureCredential) TestUtil.azureCredential();
    }
}
