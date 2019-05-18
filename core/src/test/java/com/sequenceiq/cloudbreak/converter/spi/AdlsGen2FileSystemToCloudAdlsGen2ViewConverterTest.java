package com.sequenceiq.cloudbreak.converter.spi;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsGen2View;
import com.sequenceiq.cloudbreak.common.type.filesystem.AdlsGen2FileSystem;

public class AdlsGen2FileSystemToCloudAdlsGen2ViewConverterTest {

    private static final String ACCOUNT_KEY = "a-224sv-55dfdsf-3-3444dfsf";

    private static final String ACCOUNT_NAME = "testName";

    private static final String CONTAINER_NAME = "container";

    private AdlsGen2FileSystemToCloudAdlsGen2View underTest;

    @Before
    public void setUp() {
        underTest = new AdlsGen2FileSystemToCloudAdlsGen2View();
    }

    @Test
    public void testConvertWhenPassingAdlsGen2FileSystemThenEveryNecessaryParametersShouldBePassed() {
        CloudAdlsGen2View expected = new CloudAdlsGen2View();
        expected.setAccountKey(ACCOUNT_KEY);
        expected.setAccountName(ACCOUNT_NAME);
        expected.setResourceGroupName(CONTAINER_NAME);

        CloudAdlsGen2View result = underTest.convert(createSource());

        assertEquals(expected, result);
    }

    private AdlsGen2FileSystem createSource() {
        AdlsGen2FileSystem parameters = new AdlsGen2FileSystem();
        parameters.setAccountKey(ACCOUNT_KEY);
        parameters.setAccountName(ACCOUNT_NAME);
        parameters.setStorageContainerName(CONTAINER_NAME);
        return parameters;
    }

}
