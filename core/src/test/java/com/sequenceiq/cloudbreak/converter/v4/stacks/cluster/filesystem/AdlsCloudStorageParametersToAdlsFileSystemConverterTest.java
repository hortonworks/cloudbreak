package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.filesystem;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.AdlsCloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.common.type.filesystem.AdlsFileSystem;

public class AdlsCloudStorageParametersToAdlsFileSystemConverterTest {

    private static final String TEST_ACCOUNT_NAME = "testAcountName";

    private static final String TEST_CLIENT_ID = "exampleId1234";

    private static final String TEST_CREDENTIAL = "1234567890";

    private static final String TEST_TENANT_ID = "1-2-3-4-5-6-7-8-9";

    private AdlsCloudStorageParametersV4ToAdlsFileSystemConverter underTest;

    @Before
    public void setUp() {
        underTest = new AdlsCloudStorageParametersV4ToAdlsFileSystemConverter();
    }

    @Test
    public void testConvertCheckingTheResultAdlsFileSystemIsFilledProperly() {
        AdlsFileSystem expected = createTestAdlsFileSystem();
        AdlsCloudStorageV4Parameters source = new AdlsCloudStorageV4Parameters();
        source.setAccountName(TEST_ACCOUNT_NAME);
        source.setClientId(TEST_CLIENT_ID);
        source.setCredential(TEST_CREDENTIAL);
        source.setTenantId(TEST_TENANT_ID);

        AdlsFileSystem result = underTest.convert(source);

        assertEquals(expected, result);
    }

    private AdlsFileSystem createTestAdlsFileSystem() {
        AdlsFileSystem expected = new AdlsFileSystem();
        expected.setAccountName(TEST_ACCOUNT_NAME);
        expected.setClientId(TEST_CLIENT_ID);
        expected.setCredential(TEST_CREDENTIAL);
        expected.setTenantId(TEST_TENANT_ID);
        return expected;
    }

}