package com.sequenceiq.cloudbreak.cloud.openstack.common;

import org.junit.Assert;
import org.junit.Test;

public class ConversionUtilTest {

    @Test
    public void testConvertToGBWhenInteger() {
        String actual = ConversionUtil.convertToGB("1024");
        Assert.assertEquals("1.0", actual);
    }

    @Test
    public void testConvertToGBWhenTwoDecimal() {
        String actual = ConversionUtil.convertToGB("1095.68");
        Assert.assertEquals("1.07", actual);
    }

    @Test
    public void testConvertToGBWhenOneDecimal() {
        String actual = ConversionUtil.convertToGB("1126.4");
        Assert.assertEquals("1.1", actual);
    }

    @Test
    public void testConvertToGBWhenMoreYhanFiveDecimal() {
        String actual = ConversionUtil.convertToGB("1024.022528");
        Assert.assertEquals("1.00002", actual);
    }

    @Test
    public void testConvertToGBWhenUnderOneGB() {
        String actual = ConversionUtil.convertToGB("128");
        Assert.assertEquals("0.125", actual);
    }
}
