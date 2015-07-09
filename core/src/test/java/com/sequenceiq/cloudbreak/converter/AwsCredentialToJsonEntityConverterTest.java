package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.CredentialResponse;
import com.sequenceiq.cloudbreak.domain.AwsCredential;

public class AwsCredentialToJsonEntityConverterTest extends AbstractEntityConverterTest<AwsCredential> {

    private AwsCredentialToJsonConverter underTest;

    @Before
    public void setUp() {
        underTest = new AwsCredentialToJsonConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        CredentialResponse response = underTest.convert(getSource());
        // THEN
        assertEquals(TestUtil.DUMMY_DESCRIPTION, response.getDescription());
        assertAllFieldsNotNull(response);
    }

    @Test
    public void testConvertWithoutDescription() {
        // GIVEN
        getSource().setDescription(null);
        // WHEN
        CredentialResponse response = underTest.convert(getSource());
        // THEN
        assertEquals("", response.getDescription());
        assertAllFieldsNotNull(response);
    }

    @Override
    public AwsCredential createSource() {
        return (AwsCredential) TestUtil.awsCredential();
    }
}
