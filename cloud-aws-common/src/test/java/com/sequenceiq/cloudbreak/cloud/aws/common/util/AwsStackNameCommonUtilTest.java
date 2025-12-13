package com.sequenceiq.cloudbreak.cloud.aws.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;

@ExtendWith(MockitoExtension.class)
public class AwsStackNameCommonUtilTest {

    private static final int MAX_RESOURCE_NAME_LENGTH = 50;

    private static final long PRIVATE_ID = 233L;

    @InjectMocks
    private AwsStackNameCommonUtil underTest;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(underTest, "maxResourceNameLength", MAX_RESOURCE_NAME_LENGTH);
    }

    @Test
    public void testGetInstanceNameWhenHasGroup() {
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        CloudContext cloudContext = mock(CloudContext.class);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getName()).thenReturn("stack-name");
        String actual = underTest.getInstanceName(ac, "group-name", 1L);
        assertEquals("stack-name-group-name1", actual);
    }

    @Test
    public void testGetInstanceNameWhenEmptyGroup() {
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        IllegalArgumentException actual = assertThrows(IllegalArgumentException.class, () -> underTest.getInstanceName(ac, "", 1L));
        assertEquals("Group name cannot be empty, instance name cannot be generated", actual.getMessage());
    }

    @Test
    public void testGetInstanceNameWhenNullGroup() {
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        IllegalArgumentException actual = assertThrows(IllegalArgumentException.class, () -> underTest.getInstanceName(ac, null, 1L));
        assertEquals("Group name cannot be empty, instance name cannot be generated", actual.getMessage());
    }

    @Test
    public void testGetInstanceNameWhenNameTooLong() {
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        CloudContext cloudContext = mock(CloudContext.class);
        when(ac.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getName()).thenReturn("very-very-very-long-stack-name-for-cutting");
        String actual = underTest.getInstanceName(ac, "group-name", PRIVATE_ID);
        assertEquals("very-very-very-long-stack-name-for-c-group-name233", actual);
        assertEquals(MAX_RESOURCE_NAME_LENGTH, actual.length());
    }
}
