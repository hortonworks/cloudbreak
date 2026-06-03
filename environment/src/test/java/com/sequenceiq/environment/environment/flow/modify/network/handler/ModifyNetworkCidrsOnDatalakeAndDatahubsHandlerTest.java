package com.sequenceiq.environment.environment.flow.modify.network.handler;

import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationHandlerSelectors.MODIFY_NETWORK_CIDRS_ON_DATALAKE_AND_DATAHUBS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors.FAILED_MODIFY_NETWORK_CIDRS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors.FINISH_MODIFY_NETWORK_CIDRS_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationEvent;
import com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationFailureEvent;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ModifyNetworkCidrsOnDatalakeAndDatahubsHandlerTest {
    private static final long ENV_ID = 1L;

    private static final String ENV_NAME = "envName";

    private static final String ENV_CRN = "envCrn";

    private static final List<String> NETWORK_CIDRS = List.of("10.84.128.0/17", "10.84.0.0/17");

    @Mock
    private StackService stackService;

    @InjectMocks
    private ModifyNetworkCidrsOnDatalakeAndDatahubsHandler underTest;

    private HandlerEvent<EnvNetworkCidrsModificationEvent> event;

    @BeforeEach
    void setUp() {
        EnvNetworkCidrsModificationEvent request = EnvNetworkCidrsModificationEvent.builder()
                .withSelector(MODIFY_NETWORK_CIDRS_ON_DATALAKE_AND_DATAHUBS_EVENT.selector())
                .withResourceId(ENV_ID)
                .withResourceName(ENV_NAME)
                .withResourceCrn(ENV_CRN)
                .withNetworkCidrs(NETWORK_CIDRS)
                .build();
        event = new HandlerEvent<>(new Event<>(request));
    }

    @Test
    void testDoAcceptSuccess() {
        Selectable result = underTest.doAccept(event);

        assertInstanceOf(EnvNetworkCidrsModificationEvent.class, result);
        assertEquals(FINISH_MODIFY_NETWORK_CIDRS_EVENT.name(), result.getSelector());
        verify(stackService).updateNetworkCidrsForEnvironment(ENV_CRN, NETWORK_CIDRS);
        verifyNoMoreInteractions(stackService);
    }

    @Test
    void testDoAcceptFailure() {
        doThrow(new RuntimeException("error")).when(stackService).updateNetworkCidrsForEnvironment(ENV_CRN, NETWORK_CIDRS);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(EnvNetworkCidrsModificationFailureEvent.class, result);
        assertEquals(FAILED_MODIFY_NETWORK_CIDRS_EVENT.name(), result.getSelector());
    }
}
