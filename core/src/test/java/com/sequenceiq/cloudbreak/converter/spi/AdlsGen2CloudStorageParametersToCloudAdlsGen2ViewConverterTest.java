package com.sequenceiq.cloudbreak.converter.spi;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.adls.AdlsGen2CloudStorageParameters;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsGen2View;

public class AdlsGen2CloudStorageParametersToCloudAdlsGen2ViewConverterTest {

    private static final String ACCOUNT_KEY = "a-224sv-55dfdsf-3-3444dfsf";

    private static final String ACCOUNT_NAME = "testName";

    private AdlsGen2CloudStorageParametersV4ToCloudAdlsGen2ViewConverter underTest;

    @Before
    public void setUp() {
        underTest = new AdlsGen2CloudStorageParametersV4ToCloudAdlsGen2ViewConverter();
    }

    @Test
    public void testConvertWhenPassingAdlsGen2CloudStorageParametersThenEveryNecessaryParametersShouldBePassed() {
        CloudAdlsGen2View expected = new CloudAdlsGen2View();
        expected.setAccountKey(ACCOUNT_KEY);
        expected.setAccountName(ACCOUNT_NAME);
        expected.setResourceGroupName(null);

        CloudAdlsGen2View result = underTest.convert(createSource());

        assertEquals(expected, result);
    }

    private AdlsGen2CloudStorageParameters createSource() {
        AdlsGen2CloudStorageParameters parameters = new AdlsGen2CloudStorageParameters();
        parameters.setAccountKey(ACCOUNT_KEY);
        parameters.setAccountName(ACCOUNT_NAME);
        return parameters;
    }
}
