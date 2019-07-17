package com.sequenceiq.cloudbreak.cloud.azure.util;

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

public class CustomVMImageNameProviderTest {
    private static final String AZURE_IMAGE_NAME_REGULAR_EXPRESSION = "^[^_\\W][\\w-._]{0,79}(?<![-.])$";

    private static final Pattern IMAGE_NAME_PATTERN = Pattern.compile(AZURE_IMAGE_NAME_REGULAR_EXPRESSION);

    @Test
    public void testNameGenerationWhenRegionAndWhdNameWithDelimiterDoesNotExceedMaximumLength() {
        String vhdName = "cb-hdp-26-1907121707-osDisk.cc5baaaa-717e-4551-b7ae-aaaa03c72a6a.vhd";
        String region = "West US";

        String actual = CustomVMImageNameProvider.get(region, vhdName);

        Assert.assertTrue(IMAGE_NAME_PATTERN.matcher(actual).matches());
        Assert.assertEquals("cb-hdp-26-1907121707-osDisk.cc5baaaa-717e-4551-b7ae-aaaa03c72a6a.vhd-westus", actual);
    }

    @Test
    public void testNameGenerationWhenRegionAndWhdNameWithDelimiterExceedsMaximumLength() {
        String vhdName = "cb-hdp-26-1907121707-osDisk.cc5baaaa-717e-4551-b7ae-aaaa03c72a6a.vhd";
        String region = "South East Asia";

        String actual = CustomVMImageNameProvider.get(region, vhdName);

        Assert.assertTrue(IMAGE_NAME_PATTERN.matcher(actual).matches());
        Assert.assertEquals("cb-hdp-26-1907121707-osDisk.cc5baaaa-717e-4551-b7ae-aaaa03c72a6a.v-southeastasia", actual);
    }

    @Test
    public void testNameGenerationWhenRegionAndWhdNameWithDelimiterExceedsMaximumLengthAndAfterReduceEndsWithDot() {
        String vhdName = "cb-hdp-26-1907121707-osDisk.cc5baaaa-717e-4551-b7ae-aaaa03c72a6aa.vhd";
        String region = "South East Asia";

        String actual = CustomVMImageNameProvider.get(region, vhdName);

        Assert.assertTrue(IMAGE_NAME_PATTERN.matcher(actual).matches());
        Assert.assertEquals("cb-hdp-26-1907121707-osDisk.cc5baaaa-717e-4551-b7ae-aaaa03c72a6aa.-southeastasia", actual);
    }

    @Test
    public void testNameGenerationWhenRegionAndWhdNameWithDelimiterExceedsMaximumLengthAndAfterReduceEndsWithDash() {
        String vhdName = "cb-hdp-26-1907121707-osDisk.cc5baaaa-717e-4551-b7ae-aaaa03c72a6aa-.vhd";
        String region = "South East Asia";

        String actual = CustomVMImageNameProvider.get(region, vhdName);

        Assert.assertTrue(IMAGE_NAME_PATTERN.matcher(actual).matches());
        Assert.assertEquals("cb-hdp-26-1907121707-osDisk.cc5baaaa-717e-4551-b7ae-aaaa03c72a6aa--southeastasia", actual);
    }

    @Test
    public void testNameGenerationWhenRegionAndWhdNameWithDelimiterExceedsMaximumLengthAndVhdName80CharsLong() {
        String vhdName = "cb-hdp-26-1907121707-osDisk123.cc5baaaa-717e-4551-b7ae-aaaa03c72a6a-12345678.vhd";
        String region = "South East Asia";

        String actual = CustomVMImageNameProvider.get(region, vhdName);

        Assert.assertTrue(IMAGE_NAME_PATTERN.matcher(actual).matches());
        Assert.assertEquals("cb-hdp-26-1907121707-osDisk123.cc5baaaa-717e-4551-b7ae-aaaa03c72a6-southeastasia", actual);
    }
}