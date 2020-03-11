package com.sequenceiq.freeipa.flow.freeipa.user.handler;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Config;
import com.sequenceiq.freeipa.flow.freeipa.user.event.SetPasswordRequest;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.stack.StackService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.bus.Event;

import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@ExtendWith(MockitoExtension.class)
class SetPasswordHandlerTest {

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @InjectMocks
    private SetPasswordHandler underTest;

    @Test
    void testWithPasswordHashSupport() throws FreeIpaClientException {
        SetPasswordRequest request = new SetPasswordRequest(1L, "environment", "username", "userCrn", "password", Optional.empty());
        FreeIpaClient mockFreeIpaClient = newfreeIpaClient(true);
        when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockFreeIpaClient);

        underTest.accept(new Event<>(request));

        verify(mockFreeIpaClient, times(0)).userSetPasswordWithExpiration(any(), any(), any());
    }

    @Test
    void testWithoutPasswordHashSupport() throws FreeIpaClientException {
        SetPasswordRequest request = new SetPasswordRequest(1L, "environment", "username", "userCrn", "password", Optional.empty());
        FreeIpaClient mockFreeIpaClient = newfreeIpaClient(false);
        when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(mockFreeIpaClient);

        underTest.accept(new Event<>(request));

        verify(mockFreeIpaClient, times(1)).userSetPasswordWithExpiration(any(), any(), any());
    }

    private FreeIpaClient newfreeIpaClient(boolean hasPasswordHashSuppport) throws FreeIpaClientException {
        FreeIpaClient mockFreeIpaClient = mock(FreeIpaClient.class);
        Config config = mock(Config.class);
        if (hasPasswordHashSuppport) {
            when(config.getIpauserobjectclasses()).thenReturn(Set.of("cdpUserAttr"));
        } else {
            when(config.getIpauserobjectclasses()).thenReturn(Set.of());
        }
        when(mockFreeIpaClient.getConfig()).thenReturn(config);
        return mockFreeIpaClient;
    }
}