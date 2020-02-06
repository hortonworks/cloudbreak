package com.sequenceiq.redbeams.service.network;


import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.redbeams.exception.BadRequestException;

public class AwsSubnetValidatorTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private AwsSubnetValidator underTest = new AwsSubnetValidator();

    private CloudSubnet cloudSubnet1 = new CloudSubnet("subnet1", "", "AZ1", "");

    private CloudSubnet cloudSubnet2 = new CloudSubnet("subnet2", "", "AZ1", "");

    private CloudSubnet cloudSubnet3 = new CloudSubnet("subnet3", "", "AZ2", "");

    @Test
    public void testOk() {
        underTest.validate(List.of(cloudSubnet1, cloudSubnet3), 2);
    }

    @Test
    public void testOkWithMore() {
        underTest.validate(List.of(cloudSubnet1, cloudSubnet2, cloudSubnet3), 2);
    }

    @Test
    public void testNOkWithTooFewSubnet() {
        expectedEx.expect(BadRequestException.class);
        expectedEx.expectMessage("Insufficient number of subnets: at least 2 subnets required");
        underTest.validate(List.of(cloudSubnet1), 2);
    }

    @Test
    public void testNOkWithTooFewAz() {
        expectedEx.expect(BadRequestException.class);
        expectedEx.expectMessage("Subnets are in 1 different AZ, but subnets in 2 different AZs required.");
        underTest.validate(List.of(cloudSubnet1, cloudSubnet2), 2);
    }
}