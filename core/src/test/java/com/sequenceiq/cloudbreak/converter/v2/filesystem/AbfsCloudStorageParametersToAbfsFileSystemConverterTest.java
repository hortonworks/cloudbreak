package com.sequenceiq.cloudbreak.converter.v2.filesystem;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.model.filesystem.AbfsFileSystem;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.AbfsCloudStorageParameters;

public class AbfsCloudStorageParametersToAbfsFileSystemConverterTest {

    private static final String TEST_ACCOUNT_KEY = "SKIAIKPVZ4DHPE4OHRGQ";

    private static final String TEST_ACCOUNT_NAME = "testAccountName";

    private AbfsCloudStorageParametersToAbfsFileSystemConverter underTest;

    @Before
    public void setUp() {
        underTest = new AbfsCloudStorageParametersToAbfsFileSystemConverter();
    }

    @Test
    public void testConvertCheckEveryParameterHasPassedProperly() {
        AbfsFileSystem expected = new AbfsFileSystem();
        expected.setAccountKey(TEST_ACCOUNT_KEY);
        expected.setAccountName(TEST_ACCOUNT_NAME);

        AbfsFileSystem result = underTest.convert(createAbfsCloudStorageParameters());

        assertEquals(expected, result);
    }

    private AbfsCloudStorageParameters createAbfsCloudStorageParameters() {
        AbfsCloudStorageParameters wasb = new AbfsCloudStorageParameters();
        wasb.setAccountKey(TEST_ACCOUNT_KEY);
        wasb.setAccountName(TEST_ACCOUNT_NAME);
        return wasb;
    }
}
