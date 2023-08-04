package com.sequenceiq.cloudbreak.ccmimpl.ccmv2;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.support.RetryTemplate;

import com.sequenceiq.cloudbreak.ccm.exception.CcmV2Exception;

@ExtendWith(MockitoExtension.class)
class CcmV2RetryingClientTest {

    private static final String TEST_ACCOUNT_ID = "us-west-1:e7b1345f-4ae1-4594-9113-fc91f22ef8bd";

    private static final String TEST_AGENT_CRN = "testAgentCrn";

    private static final String TEST_ACCESS_KEY_ID = "testAccessKeyId";

    private static final String TEST_ENVIRONMENT_CRN = "environmentCrn";

    private static final String DOMAIN = "domain";

    private static final String KEY_ID = "key";

    private static final String HMAC = "hmac";

    @Mock
    private CcmV2Client ccmV2Client;

    @Mock
    private RetryTemplateFactory retryTemplateFactory;

    @Mock
    private RetryTemplate template;

    @InjectMocks
    private CcmV2RetryingClient underTest;

    @BeforeEach
    void setUp() {
        doThrow(CcmV2Exception.class).when(template).execute(any(), any(RecoveryCallback.class));
        when(retryTemplateFactory.getRetryTemplate()).thenReturn(template);
    }

    @Test
    void testgetOrCreateInvertingProxyWhenException() {
        assertThatThrownBy(() -> underTest.awaitReadyInvertingProxyForAccount(TEST_ACCOUNT_ID))
                .isInstanceOf(CcmV2Exception.class);
    }

    @Test
    void testRegisterAgentWhenException() {
        assertThatThrownBy(() -> underTest.registerInvertingProxyAgent(TEST_AGENT_CRN, Optional.of(TEST_ENVIRONMENT_CRN), DOMAIN, KEY_ID, Optional.of(HMAC)))
                .isInstanceOf(CcmV2Exception.class);
    }

    @Test
    void testUnRegisterAgentWhenException() {
        assertThatThrownBy(() -> underTest.deregisterInvertingProxyAgent(TEST_AGENT_CRN)).isInstanceOf(CcmV2Exception.class);
    }

    @Test
    void testListAgentsWhenException() {
        assertThatThrownBy(() -> underTest.listInvertingProxyAgents(TEST_ACCOUNT_ID, Optional.of(TEST_ENVIRONMENT_CRN))).isInstanceOf(CcmV2Exception.class);
    }

    @Test
    void testCreateAgentAccessKeyPairWhenException() {
        assertThatThrownBy(() -> underTest.createAgentAccessKeyPair(TEST_ACCOUNT_ID, TEST_AGENT_CRN, Optional.of(HMAC))).isInstanceOf(CcmV2Exception.class);
    }

    @Test
    void testDeactivateAgentAccessKeyPairWhenException() {
        assertThatThrownBy(() -> underTest.deactivateAgentAccessKeyPair(TEST_ACCOUNT_ID, TEST_ACCESS_KEY_ID)).isInstanceOf(CcmV2Exception.class);
    }
}
