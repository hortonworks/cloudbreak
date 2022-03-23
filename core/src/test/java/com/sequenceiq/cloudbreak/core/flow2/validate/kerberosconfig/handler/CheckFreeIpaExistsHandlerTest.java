package com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.handler;

import static com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_FAILED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.config.KerberosConfigValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.event.CheckFreeIpaExistsEvent;
import com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.event.ValidateKerberosConfigEvent;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
class CheckFreeIpaExistsHandlerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    private static final String ENV_CRN = "envCrn";

    @Mock
    private FreeIpaV1Endpoint freeIpaV1Endpoint;

    @Mock
    private StackViewService stackViewService;

    private StackView stackView = mock(StackView.class);

    @InjectMocks
    private CheckFreeIpaExistsHandler underTest;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @BeforeEach
    public void init() {
        when(stackView.getEnvironmentCrn()).thenReturn(ENV_CRN);
        lenient().when(stackViewService.getById(1L)).thenReturn(stackView);
    }

    @Test
    public void testSelector() {
        assertEquals(EventSelectorUtil.selector(CheckFreeIpaExistsEvent.class), underTest.selector());
    }

    @Test
    public void testDefaultFailureEvent() {
        Event<CheckFreeIpaExistsEvent> event = new Event<>(new CheckFreeIpaExistsEvent(1L));
        Exception e = new Exception();

        StackFailureEvent result = (StackFailureEvent) underTest.defaultFailureEvent(1L, e, event);

        assertEquals(VALIDATE_KERBEROS_CONFIG_FAILED_EVENT.event(), result.selector());
        assertEquals(1L, result.getResourceId());
        assertEquals(e, result.getException());
    }

    @Test
    public void testFreeIpaExists() {
        Event<CheckFreeIpaExistsEvent> event = new Event<>(new CheckFreeIpaExistsEvent(1L));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        StackEvent result = (StackEvent) ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.doAccept(new HandlerEvent<>(event)));

        assertEquals(KerberosConfigValidationEvent.FREEIPA_EXISTS_EVENT.event(), result.selector());
        assertEquals(1L, result.getResourceId());
        verify(freeIpaV1Endpoint).describeInternal(ENV_CRN, "1234");
    }

    @Test
    public void testFreeIpaDontExists() {
        Event<CheckFreeIpaExistsEvent> event = new Event<>(new CheckFreeIpaExistsEvent(1L));
        when(freeIpaV1Endpoint.describeInternal(ENV_CRN, "1234")).thenThrow(new NotFoundException());
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        ValidateKerberosConfigEvent result = (ValidateKerberosConfigEvent) ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.doAccept(new HandlerEvent<>(event)));

        assertEquals(KerberosConfigValidationEvent.VALIDATE_KERBEROS_CONFIG_EXISTS_EVENT.event(), result.selector());
        assertEquals(1L, result.getResourceId());
        assertFalse(result.doesFreeipaExistsForEnv());
    }
}