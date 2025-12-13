package com.sequenceiq.cloudbreak.cloud.azure.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

public class CustomVMImageNameProviderTest {
    private static final String AZURE_IMAGE_NAME_REGULAR_EXPRESSION = "^[^_\\W][\\w-._]{0,79}(?<![-.])$";

    private static final Pattern IMAGE_NAME_PATTERN = Pattern.compile(AZURE_IMAGE_NAME_REGULAR_EXPRESSION);

    @InjectMocks
    private CustomVMImageNameProvider underTest;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testNameGenerationWhenRegionAndWhdNameWithDelimiterDoesNotExceedMaximumLength() {
        String vhdName = "cb-hdp-26-1907121707-osDisk.cc5baaaa-717e-4551-b7ae-aaaa03c72a6a.vhd";
        String region = "West US";

        String actual = underTest.getImageNameWithRegion(region, vhdName);

        assertTrue(IMAGE_NAME_PATTERN.matcher(actual).matches());
        assertEquals("cb-hdp-26-1907121707-osDisk.cc5baaaa-717e-4551-b7ae-aaaa03c72a6a.vhd-westus", actual);
    }

    @Test
    public void testNameGenerationWhenRegionAndWhdNameWithDelimiterExceedsMaximumLength() {
        String vhdName = "cb-hdp-26-1907121707-osDisk.cc5baaaa-717e-4551-b7ae-aaaa03c72a6a.vhd";
        String region = "South East Asia";

        String actual = underTest.getImageNameWithRegion(region, vhdName);

        assertTrue(IMAGE_NAME_PATTERN.matcher(actual).matches());
        assertEquals("cb-hdp-26-1907121707-osDisk.cc5baaaa-717e-4551-b7ae-aaaa03c72a6a.v-southeastasia", actual);
    }

    @Test
    public void testNameGenerationWhenRegionAndWhdNameWithDelimiterExceedsMaximumLengthAndAfterReduceEndsWithDot() {
        String vhdName = "cb-hdp-26-1907121707-osDisk.cc5baaaa-717e-4551-b7ae-aaaa03c72a6aa.vhd";
        String region = "South East Asia";

        String actual = underTest.getImageNameWithRegion(region, vhdName);

        assertTrue(IMAGE_NAME_PATTERN.matcher(actual).matches());
        assertEquals("cb-hdp-26-1907121707-osDisk.cc5baaaa-717e-4551-b7ae-aaaa03c72a6aa.-southeastasia", actual);
    }

    @Test
    public void testNameGenerationWhenRegionAndWhdNameWithDelimiterExceedsMaximumLengthAndAfterReduceEndsWithDash() {
        String vhdName = "cb-hdp-26-1907121707-osDisk.cc5baaaa-717e-4551-b7ae-aaaa03c72a6aa-.vhd";
        String region = "South East Asia";

        String actual = underTest.getImageNameWithRegion(region, vhdName);

        assertTrue(IMAGE_NAME_PATTERN.matcher(actual).matches());
        assertEquals("cb-hdp-26-1907121707-osDisk.cc5baaaa-717e-4551-b7ae-aaaa03c72a6aa--southeastasia", actual);
    }

    @Test
    public void testNameGenerationWhenRegionAndWhdNameWithDelimiterExceedsMaximumLengthAndVhdName80CharsLong() {
        String vhdName = "cb-hdp-26-1907121707-osDisk123.cc5baaaa-717e-4551-b7ae-aaaa03c72a6a-12345678.vhd";
        String region = "South East Asia";

        String actual = underTest.getImageNameWithRegion(region, vhdName);

        assertTrue(IMAGE_NAME_PATTERN.matcher(actual).matches());
        assertEquals("cb-hdp-26-1907121707-osDisk123.cc5baaaa-717e-4551-b7ae-aaaa03c72a6-southeastasia", actual);
    }

    @Test
    public void getImageNameFromConnectionStringWithSASToken() {
        String url = "https://sequenceiqwestus2.blob.core.windows.net/test/cb-hdp-31-1911052024.vhd"
                + "?sp=rl&st=2020-10-26T11:45:18Z&se=2020-10-27T11:45:18Z&sv=2019-12-12&sr=b&sig=G6hkDHn7GezwXBzZhQBNLZD5kI3LXgWMvlxGbm1T8WU%3D";
        String result = underTest.getImageNameFromConnectionString(url);
        assertEquals("cb-hdp-31-1911052024.vhd", result);
    }

    @Test
    public void getImageNameFromConnectionStringWithoutSASToken() {
        String url = "https://sequenceiqwestus2.blob.core.windows.net/test/cb-hdp-31-1911052024.vhd";
        String result = underTest.getImageNameFromConnectionString(url);
        assertEquals("cb-hdp-31-1911052024.vhd", result);
    }

}