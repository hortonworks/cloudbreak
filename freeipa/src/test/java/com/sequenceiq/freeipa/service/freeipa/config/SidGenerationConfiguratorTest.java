package com.sequenceiq.freeipa.service.freeipa.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.util.CheckedRunnable;
import com.sequenceiq.cloudbreak.util.CheckedSupplier;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientService;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.host.Rhel8ClientHelper;

@ExtendWith(MockitoExtension.class)
class SidGenerationConfiguratorTest {

    @Mock
    private Rhel8ClientHelper rhel8ClientHelper;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private RetryableFreeIpaClientService retryService;

    @InjectMocks
    private SidGenerationConfigurator underTest;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(retryService.invokeWithRetries(any(CheckedSupplier.class)))
                .thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0, CheckedSupplier.class).get());
        lenient().doAnswer(invocationOnMock -> {
            invocationOnMock.getArgument(0, CheckedRunnable.class).run();
            return null;
        }).when(retryService).invokeWithRetries(any(CheckedRunnable.class));
    }

    @Test
    void testConnectedToRhel8() throws FreeIpaClientException {
        Stack stack = new Stack();
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(rhel8ClientHelper.isClientConnectedToRhel8(stack, freeIpaClient)).thenReturn(true);

        underTest.enableAndTriggerSidGeneration(stack, freeIpaClient);

        verify(freeIpaClient).enableAndTriggerSidGeneration();
        verify(rhel8ClientHelper, never()).findRhel8Instance(stack);
    }

    @Test
    void testConnectToRhel8() throws FreeIpaClientException {
        Stack stack = new Stack();
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(rhel8ClientHelper.isClientConnectedToRhel8(stack, freeIpaClient)).thenReturn(false);
        when(rhel8ClientHelper.findRhel8Instance(stack)).thenReturn(Optional.of("rhel8ipa"));
        FreeIpaClient rhel8FreeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForInstance(stack, "rhel8ipa")).thenReturn(rhel8FreeIpaClient);

        underTest.enableAndTriggerSidGeneration(stack, freeIpaClient);

        verify(rhel8FreeIpaClient).enableAndTriggerSidGeneration();
        verifyNoInteractions(freeIpaClient);
    }

    @Test
    void testNoRhel8() {
        Stack stack = new Stack();
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(rhel8ClientHelper.isClientConnectedToRhel8(stack, freeIpaClient)).thenReturn(false);
        when(rhel8ClientHelper.findRhel8Instance(stack)).thenReturn(Optional.empty());

        underTest.enableAndTriggerSidGeneration(stack, freeIpaClient);

        verifyNoInteractions(freeIpaClient);
        verifyNoInteractions(retryService);
    }

    @Test
    void testExceptionHandled() throws FreeIpaClientException {
        Stack stack = new Stack();
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(rhel8ClientHelper.isClientConnectedToRhel8(stack, freeIpaClient)).thenReturn(true);
        doThrow(new FreeIpaClientException("asdf")).when(freeIpaClient).enableAndTriggerSidGeneration();

        underTest.enableAndTriggerSidGeneration(stack, freeIpaClient);
    }
}