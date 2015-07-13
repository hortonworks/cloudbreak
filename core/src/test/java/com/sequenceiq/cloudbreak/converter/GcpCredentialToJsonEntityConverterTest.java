package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.CredentialResponse;
import com.sequenceiq.cloudbreak.controller.validation.GcpCredentialParam;
import com.sequenceiq.cloudbreak.domain.GcpCredential;

public class GcpCredentialToJsonEntityConverterTest extends AbstractEntityConverterTest<GcpCredential> {

    private GcpCredentialToJsonConverter underTest;

    @Before
    public void setUp() {
        underTest = new GcpCredentialToJsonConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        CredentialResponse result = underTest.convert(getSource());
        // THEN
        assertEquals("dummyProjectId", result.getParameters().get(GcpCredentialParam.PROJECTID.getName()));
        assertAllFieldsNotNull(result, Arrays.asList("publicKey"));
    }

    @Test
    public void testConvertWithoutDescription() {
        // GIVEN
        getSource().setDescription(null);
        // WHEN
        CredentialResponse result = underTest.convert(getSource());
        // THEN
        assertEquals("", result.getDescription());
        assertAllFieldsNotNull(result, Arrays.asList("publicKey"));
    }

    @Override
    public GcpCredential createSource() {
        return (GcpCredential) TestUtil.gcpCredential();
    }
}
