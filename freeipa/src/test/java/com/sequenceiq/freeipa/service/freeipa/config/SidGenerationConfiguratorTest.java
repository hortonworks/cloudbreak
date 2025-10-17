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
import com.sequenceiq.freeipa.dto.SidGeneration;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.host.RhelClientHelper;

@ExtendWith(MockitoExtension.class)
class SidGenerationConfiguratorTest {

    @Mock
    private RhelClientHelper rhelClientHelper;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private RetryableFreeIpaClientService retryService;

    @Mock
    private FreeIpaService freeIpaService;

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
        when(rhelClientHelper.isClientConnectedToRhel(stack, freeIpaClient)).thenReturn(true);
        FreeIpa freeIpa = new FreeIpa();
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);

        underTest.enableAndTriggerSidGeneration(stack, freeIpaClient);

        verify(freeIpaClient).enableAndTriggerSidGeneration();
        verify(rhelClientHelper, never()).findRhelInstance(stack);
        verify(freeIpaService).save(freeIpa);
    }

    @Test
    void testConnectToRhel8() throws FreeIpaClientException {
        Stack stack = new Stack();
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(rhelClientHelper.isClientConnectedToRhel(stack, freeIpaClient)).thenReturn(false);
        when(rhelClientHelper.findRhelInstance(stack)).thenReturn(Optional.of("rhel8ipa"));
        FreeIpaClient rhel8FreeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForInstance(stack, "rhel8ipa")).thenReturn(rhel8FreeIpaClient);
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setSidGeneration(SidGeneration.DISABLED);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);

        underTest.enableAndTriggerSidGeneration(stack, freeIpaClient);

        verify(rhel8FreeIpaClient).enableAndTriggerSidGeneration();
        verifyNoInteractions(freeIpaClient);
        verify(freeIpaService).save(freeIpa);
    }

    @Test
    void testNoRhel8() {
        Stack stack = new Stack();
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(rhelClientHelper.isClientConnectedToRhel(stack, freeIpaClient)).thenReturn(false);
        when(rhelClientHelper.findRhelInstance(stack)).thenReturn(Optional.empty());
        when(freeIpaService.findByStack(stack)).thenReturn(new FreeIpa());

        underTest.enableAndTriggerSidGeneration(stack, freeIpaClient);

        verifyNoInteractions(freeIpaClient);
        verifyNoInteractions(retryService);
        verify(freeIpaService, never()).save(any());
    }

    @Test
    void testExceptionHandled() throws FreeIpaClientException {
        Stack stack = new Stack();
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(rhelClientHelper.isClientConnectedToRhel(stack, freeIpaClient)).thenReturn(true);
        doThrow(new FreeIpaClientException("asdf")).when(freeIpaClient).enableAndTriggerSidGeneration();
        when(freeIpaService.findByStack(stack)).thenReturn(new FreeIpa());

        underTest.enableAndTriggerSidGeneration(stack, freeIpaClient);

        verify(freeIpaService, never()).save(any());
    }

    @Test
    void testSidAlreadyEnabled() throws FreeIpaClientException {
        Stack stack = new Stack();
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setSidGeneration(SidGeneration.ENABLED);
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);

        underTest.enableAndTriggerSidGeneration(stack, freeIpaClient);

        verify(freeIpaClient, never()).enableAndTriggerSidGeneration();
        verify(rhelClientHelper, never()).findRhelInstance(stack);
        verify(freeIpaService, never()).save(freeIpa);
    }
}