package com.sequenceiq.cloudbreak.cloud.azure.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AzureResourceNameServiceTest {

    private static final String STACK_NAME = "stackName";

    private static final String GROUP_NAME = "groupName";

    private static final String HASH = "hash";

    private static final Long STACK_ID = 123L;

    private static final Long PRIVATE_ID = 0L;

    private static final int COUNT = 1;

    @InjectMocks
    private AzureResourceNameService underTest;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(underTest, "maxResourceNameLength", 70);
    }

    @Test
    void testVolumeSet() {
        String resourceName = underTest.volumeSet(STACK_NAME, GROUP_NAME, PRIVATE_ID, HASH);
        assertTrue(resourceName.startsWith("stackname-g-0-"));
    }

    @Test
    void testAttachedDisk() {
        String resourceName = underTest.attachedDisk(STACK_NAME, GROUP_NAME, PRIVATE_ID, COUNT, HASH);
        assertTrue(resourceName.startsWith("stackname-g-0-1-"));
    }
}