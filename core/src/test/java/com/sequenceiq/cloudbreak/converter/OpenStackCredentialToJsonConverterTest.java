package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.CredentialResponse;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.OpenStackCredential;

public class OpenStackCredentialToJsonConverterTest extends AbstractEntityConverterTest<OpenStackCredential> {

    private OpenStackCredentialToJsonConverter underTest;

    @Before
    public void setUp() {
        underTest = new OpenStackCredentialToJsonConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        CredentialResponse result = underTest.convert(getSource());
        // THEN
        assertEquals(CloudPlatform.OPENSTACK, result.getCloudPlatform());
        assertEquals("dummyName", result.getName());
    }

    @Override
    public OpenStackCredential createSource() {
        return (OpenStackCredential) TestUtil.openStackCredential();
    }
}
