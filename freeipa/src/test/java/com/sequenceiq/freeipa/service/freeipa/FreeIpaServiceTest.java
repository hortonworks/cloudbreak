package com.sequenceiq.freeipa.service.freeipa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.dal.ResourceBasicView;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.CrnService;

@ExtendWith(MockitoExtension.class)
public class FreeIpaServiceTest {

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:default:environment:174e680c-5a0a-48be-88ac-8b5498c30311";

    private static final String FREEIPA_CRN = "crn:cdp:freeipa:us-west-1:default:freeipa:e56c255e-fa78-4491-8224-99aef34d77a8";

    private static final String CURRENT_USER = "tesuser";

    @Mock
    private StackService stackService;

    @Mock
    private CrnService crnService;

    @Mock
    private FlowEndpoint flowEndpoint;

    @InjectMocks
    private FreeIpaService underTest;

    @Test
    void testGetResourceIdByResourceCrnForEnvCrn() {
        when(crnService.getCurrentAccountId()).thenReturn(CURRENT_USER);
        Stack stack = mock(Stack.class);
        Long expectedResourceId = Long.valueOf(100);
        when(stack.getId()).thenReturn(expectedResourceId);
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, CURRENT_USER)).thenReturn(stack);
        Long resourceId = underTest.getResourceIdByResourceCrn(ENV_CRN);
        assertEquals(expectedResourceId, resourceId);
        verify(stackService, times(0)).getResourceBasicViewByCrn(any());
    }

    @Test
    void testGetResourceIdByResourceCrnForFreeIpaCrn() {
        ResourceBasicView resourceBasicView = mock(ResourceBasicView.class);
        Long expectedResourceId = Long.valueOf(100);
        when(resourceBasicView.getId()).thenReturn(expectedResourceId);
        when(stackService.getResourceBasicViewByCrn(FREEIPA_CRN)).thenReturn(resourceBasicView);
        Long resourceId = underTest.getResourceIdByResourceCrn(FREEIPA_CRN);
        assertEquals(expectedResourceId, resourceId);
        verify(stackService, times(0)).getByEnvironmentCrnAndAccountId(any(), any());
    }
}
