package com.sequenceiq.cloudbreak.idbmms.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class IdbmmsConfigTest {

    @InjectMocks
    private IdbmmsConfig underTest;

    @Test
    void isConfiguredTestNullEndpoint() {
        assertThat(underTest.isConfigured()).isFalse();
    }

    @Test
    void isConfiguredTestEmptyEndpoint() {
        ReflectionTestUtils.setField(underTest, "host", "");

        assertThat(underTest.isConfigured()).isFalse();
    }

    @Test
    void isConfiguredTestGoodEndpoint() {
        ReflectionTestUtils.setField(underTest, "host", "myhost.com");

        assertThat(underTest.isConfigured()).isTrue();
    }

}