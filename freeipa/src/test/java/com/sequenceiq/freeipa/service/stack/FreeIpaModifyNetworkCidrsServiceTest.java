package com.sequenceiq.freeipa.service.stack;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.freeipa.entity.Stack;

@ExtendWith(MockitoExtension.class)
class FreeIpaModifyNetworkCidrsServiceTest {

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String ACCOUNT_ID = "accountId";

    @Mock
    private StackService stackService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private Stack stack;

    @InjectMocks
    private FreeIpaModifyNetworkCidrsService underTest;

    @Test
    void modifyNetworkCidrs() {
        List<String> networkCidrs = List.of("10.84.128.0/17", "10.84.0.0/17");

        when(stackService.getFreeIpaStackWithMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);

        underTest.modifyNetworkCidrs(ENVIRONMENT_CRN, ACCOUNT_ID, networkCidrs);

        verify(stackUpdater).updateNetworkCidrs(stack, networkCidrs);
    }

    @Test
    void modifyNetworkCidrsWhenStackNotFound() {
        List<String> networkCidrs = List.of("10.84.128.0/17", "10.84.0.0/17");

        when(stackService.getFreeIpaStackWithMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenThrow(new NotFoundException("FreeIPA stack not found"));

        assertThrows(NotFoundException.class, () -> underTest.modifyNetworkCidrs(ENVIRONMENT_CRN, ACCOUNT_ID, networkCidrs));

        verifyNoInteractions(stackUpdater);
    }
}