package com.sequenceiq.cloudbreak.idbmms.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

public class IdbmmsConfigTest {

    @InjectMocks
    private IdbmmsConfig underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testIsConfiguredNullEndpoint() {
        assertThat(underTest.isConfigured()).isFalse();
    }

    @Test
    public void testIsConfiguredEmptyEndpoint() {
        ReflectionTestUtils.setField(underTest, "endpoint", "");

        assertThat(underTest.isConfigured()).isFalse();
    }

    @Test
    public void testIsConfiguredGoodEndpoint() {
        ReflectionTestUtils.setField(underTest, "endpoint", "myhost.com");

        assertThat(underTest.isConfigured()).isTrue();
    }

}