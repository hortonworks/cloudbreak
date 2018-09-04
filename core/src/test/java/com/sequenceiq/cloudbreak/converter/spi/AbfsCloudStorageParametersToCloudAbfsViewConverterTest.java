package com.sequenceiq.cloudbreak.converter.spi;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.model.v2.filesystem.AbfsCloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAbfsView;

public class AbfsCloudStorageParametersToCloudAbfsViewConverterTest {

    private static final String ACCOUNT_KEY = "a-224sv-55dfdsf-3-3444dfsf";

    private static final String ACCOUNT_NAME = "testName";

    private AbfsCloudStorageParametersToCloudAbfsViewConverter underTest;

    @Before
    public void setUp() {
        underTest = new AbfsCloudStorageParametersToCloudAbfsViewConverter();
    }

    @Test
    public void testConvertWhenPassingAbfsCloudStorageParametersThenEveryNecessaryParametersShouldBePassed() {
        CloudAbfsView expected = new CloudAbfsView();
        expected.setAccountKey(ACCOUNT_KEY);
        expected.setAccountName(ACCOUNT_NAME);
        expected.setResourceGroupName(null);

        CloudAbfsView result = underTest.convert(createSource());

        assertEquals(expected, result);
    }

    private AbfsCloudStorageParameters createSource() {
        AbfsCloudStorageParameters parameters = new AbfsCloudStorageParameters();
        parameters.setAccountKey(ACCOUNT_KEY);
        parameters.setAccountName(ACCOUNT_NAME);
        return parameters;
    }
}
