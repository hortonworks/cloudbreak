package com.sequenceiq.cloudbreak.idbmms.config;

import static org.junit.Assert.assertFalse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdbmmsConfigTest {

    @InjectMocks
    private IdbmmsConfig underTest;

    @Test
    void isConfiguredTestNullEndpoint() {
        assertFalse(underTest.isConfigured());
    }
}