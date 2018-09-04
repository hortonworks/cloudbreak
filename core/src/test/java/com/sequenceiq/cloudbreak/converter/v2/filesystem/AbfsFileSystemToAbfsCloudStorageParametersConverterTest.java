package com.sequenceiq.cloudbreak.converter.v2.filesystem;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.model.filesystem.AbfsFileSystem;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.AbfsCloudStorageParameters;

public class AbfsFileSystemToAbfsCloudStorageParametersConverterTest {

    private static final String TEST_ACCOUNT_KEY = "a-224sv-55dfdsf-3-3444dfsf";

    private static final String TEST_ACCOUNT_NAME = "testAccountName";

    private AbfsFileSystemToAbfsCloudStorageParametersConverter underTest;

    @Before
    public void setUp() {
        underTest = new AbfsFileSystemToAbfsCloudStorageParametersConverter();
    }

    @Test
    public void testConvertCheckEveryParameterHasPassedProperly() {
        AbfsCloudStorageParameters expected = new AbfsCloudStorageParameters();
        expected.setAccountKey(TEST_ACCOUNT_KEY);
        expected.setAccountName(TEST_ACCOUNT_NAME);

        AbfsCloudStorageParameters result = underTest.convert(createAbfsFileSystem());

        assertEquals(expected, result);
    }

    private AbfsFileSystem createAbfsFileSystem() {
        AbfsFileSystem wasb = new AbfsFileSystem();
        wasb.setAccountKey(TEST_ACCOUNT_KEY);
        wasb.setAccountName(TEST_ACCOUNT_NAME);
        return wasb;
    }
}
