package com.sequenceiq.cloudbreak.sdx.pdl.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PdlSdxDhTearDownServiceTest {
    @InjectMocks
    private PdlSdxDhTearDownService underTest;

    @Test
    public void testTearDownDataHub() {
        underTest.tearDownDataHub("sdx", "dh");
    }
}
