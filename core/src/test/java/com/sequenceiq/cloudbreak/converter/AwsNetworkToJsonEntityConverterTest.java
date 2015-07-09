package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.NetworkJson;
import com.sequenceiq.cloudbreak.controller.validation.AwsNetworkParam;
import com.sequenceiq.cloudbreak.domain.AwsNetwork;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class AwsNetworkToJsonEntityConverterTest extends AbstractEntityConverterTest<AwsNetwork> {

    private AwsNetworkToJsonConverter underTest;


    @Before
    public void setUp() {
        underTest = new AwsNetworkToJsonConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        NetworkJson result = underTest.convert(getSource());
        // THEN
        assertEquals(CloudPlatform.AWS, result.getCloudPlatform());
        assertEquals(TestUtil.DUMMY_VPC_ID, result.getParameters().get(AwsNetworkParam.VPC_ID.getName()));
        assertAllFieldsNotNull(result);
    }

    @Override
    public AwsNetwork createSource() {
        return TestUtil.awsNetwork("10.0.0.1/16");
    }
}
