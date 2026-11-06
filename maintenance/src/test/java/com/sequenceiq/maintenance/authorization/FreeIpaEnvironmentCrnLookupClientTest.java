package com.sequenceiq.maintenance.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaInternalV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaEnvironmentCrnResponse;

@ExtendWith(MockitoExtension.class)
class FreeIpaEnvironmentCrnLookupClientTest {

    private static final String ACCOUNT_ID = "acc-1";

    private static final String FREEIPA_CRN = "crn:cdp:freeipa:us-west-1:acc-1:freeipa:fi-1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc-1:environment:env-1";

    @Mock
    private FreeIpaInternalV1Endpoint freeIpaInternalV1Endpoint;

    @InjectMocks
    private FreeIpaEnvironmentCrnLookupClient underTest;

    private MockedStatic<ThreadBasedUserCrnProvider> userCrnProvider;

    @BeforeEach
    void setUp() {
        userCrnProvider = org.mockito.Mockito.mockStatic(ThreadBasedUserCrnProvider.class);
        userCrnProvider.when(() -> ThreadBasedUserCrnProvider.doAsInternalActor(any(Supplier.class), eq(ACCOUNT_ID)))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
    }

    @AfterEach
    void tearDown() {
        userCrnProvider.close();
    }

    @Test
    void lookupUsesInternalActorAndReturnsEnvironmentCrn() {
        when(freeIpaInternalV1Endpoint.getEnvironmentCrnByResourceCrn(FREEIPA_CRN))
                .thenReturn(new FreeIpaEnvironmentCrnResponse(ENV_CRN));

        assertThat(underTest.lookupEnvironmentCrn(FREEIPA_CRN, ACCOUNT_ID)).contains(ENV_CRN);

        verify(freeIpaInternalV1Endpoint).getEnvironmentCrnByResourceCrn(FREEIPA_CRN);
        userCrnProvider.verify(() -> ThreadBasedUserCrnProvider.doAsInternalActor(any(Supplier.class), eq(ACCOUNT_ID)));
    }

    @Test
    void lookupReturnsEmptyWhenFreeIpaHasNoStack() {
        when(freeIpaInternalV1Endpoint.getEnvironmentCrnByResourceCrn(FREEIPA_CRN)).thenReturn(null);

        assertThat(underTest.lookupEnvironmentCrn(FREEIPA_CRN, ACCOUNT_ID)).isEmpty();
    }
}
