package com.sequenceiq.cloudbreak.cloud.gcp;

import static org.mockito.Mockito.when;

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;

@ExtendWith(MockitoExtension.class)
public class GcpTagValidatorTest {

    @Mock
    private GcpLabelUtil gcpLabelUtil;

    @Mock
    private GcpPlatformParameters platformParameters;

    @InjectMocks
    private GcpTagValidator underTest;

    private TagSpecification tagSpecification = new TagSpecification(
            1,
            2,
            3,
            "apple0",
            4,
            5,
            "apple1");

    @BeforeEach
    public void before() {
        when(platformParameters.tagSpecification()).thenReturn(tagSpecification);
        underTest.init();
    }

    @Test
    public void testGetKeyValidatorShouldReturnValuePattern() {
        Pattern expected = Pattern.compile("apple0");
        Assert.assertEquals(underTest.getKeyValidator().pattern(), expected.pattern());
    }

    @Test
    public void testGetValueValidatorShouldReturnValuePattern() {
        Pattern expected = Pattern.compile("apple1");
        Assert.assertEquals(underTest.getValueValidator().pattern(), expected.pattern());
    }

    @Test
    public void testGetTagSpecificationShouldReturnTagSpecification() {
        Assert.assertEquals(underTest.getTagSpecification(), tagSpecification);
    }

    @Test
    public void testTransformGoogleLabel() {
        when(gcpLabelUtil.transformLabelKeyOrValue("apple")).thenReturn("apple1");
        Assert.assertEquals(underTest.transform("apple"), "apple1");
    }
}