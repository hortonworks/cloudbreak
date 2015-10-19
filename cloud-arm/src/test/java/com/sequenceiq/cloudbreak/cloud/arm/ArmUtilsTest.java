package com.sequenceiq.cloudbreak.cloud.arm;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_MAX_AZURE_RESOURCE_NAME_LENGTH;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;


public class ArmUtilsTest {

    private ArmUtils subject;
    private final String maxResourceNameLength = CB_MAX_AZURE_RESOURCE_NAME_LENGTH;

    @Before
    public void setUp() {
        subject = new ArmUtils();
        ReflectionTestUtils.setField(subject, "maxResourceNameLength", Integer.parseInt(maxResourceNameLength));
    }

    @Test
    public void shouldAdjustResourceNameLengthIfItsTooLong() {
        //GIVEN
        CloudContext context = new CloudContext(7899L, "thisisaverylongazureresourcenamewhichneedstobeshortened", "dummy1", "dummy2");

        //WHEN
        String testResult = subject.getStackName(context);

        //THEN
        Assert.assertNotNull("The generated name must not be null!", testResult);
        Assert.assertEquals("The resource name is not the excepted one!", "thisisaverylongazureresourcenamewhichneedstobe7899", testResult);
        Assert.assertTrue("The resource name length is wrong", testResult.length() == Integer.parseInt(maxResourceNameLength));

    }
}
