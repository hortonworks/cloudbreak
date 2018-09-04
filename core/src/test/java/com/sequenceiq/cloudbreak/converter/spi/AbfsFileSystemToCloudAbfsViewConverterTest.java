package com.sequenceiq.cloudbreak.converter.spi;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.model.filesystem.AbfsFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAbfsView;

public class AbfsFileSystemToCloudAbfsViewConverterTest {

    private static final String ACCOUNT_KEY = "a-224sv-55dfdsf-3-3444dfsf";

    private static final String ACCOUNT_NAME = "testName";

    private static final String CONTAINER_NAME = "container";

    private AbfsFileSystemToCloudAbfsView underTest;

    @Before
    public void setUp() {
        underTest = new AbfsFileSystemToCloudAbfsView();
    }

    @Test
    public void testConvertWhenPassingAbfsFileSystemThenEveryNecessaryParametersShouldBePassed() {
        CloudAbfsView expected = new CloudAbfsView();
        expected.setAccountKey(ACCOUNT_KEY);
        expected.setAccountName(ACCOUNT_NAME);
        expected.setResourceGroupName(CONTAINER_NAME);

        CloudAbfsView result = underTest.convert(createSource());

        assertEquals(expected, result);
    }

    private AbfsFileSystem createSource() {
        AbfsFileSystem parameters = new AbfsFileSystem();
        parameters.setAccountKey(ACCOUNT_KEY);
        parameters.setAccountName(ACCOUNT_NAME);
        parameters.setStorageContainerName(CONTAINER_NAME);
        return parameters;
    }

}
