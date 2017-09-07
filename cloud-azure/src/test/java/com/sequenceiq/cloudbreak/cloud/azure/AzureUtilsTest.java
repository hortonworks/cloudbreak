package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;

public class AzureUtilsTest {

    private static final String MAX_RESOURCE_NAME_LENGTH = "50";

    private AzureUtils subject;

    @Before
    public void setUp() {
        subject = new AzureUtils();
        ReflectionTestUtils.setField(subject, "maxResourceNameLength", Integer.parseInt(MAX_RESOURCE_NAME_LENGTH));
    }

    @Test
    public void shouldAdjustResourceNameLengthIfItsTooLong() {
        //GIVEN
        CloudContext context = new CloudContext(7899L, "thisisaverylongazureresourcenamewhichneedstobeshortened", "dummy1", "dummy2");

        //WHEN
        String testResult = subject.getStackName(context);

        //THEN
        Assert.assertNotNull("The generated name must not be null!", testResult);
        assertEquals("The resource name is not the excepted one!", "thisisaverylongazureresourcenamewhichneedstobe7899", testResult);
        Assert.assertTrue("The resource name length is wrong", testResult.length() == Integer.parseInt(MAX_RESOURCE_NAME_LENGTH));

    }
}
