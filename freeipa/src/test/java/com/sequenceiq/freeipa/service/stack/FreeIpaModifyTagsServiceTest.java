package com.sequenceiq.freeipa.service.stack;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.entity.Stack;

@ExtendWith(MockitoExtension.class)
class FreeIpaModifyTagsServiceTest {
    @Mock
    private StackService stackService;

    @Mock
    private StackUpdater stackUpdater;

    @InjectMocks
    private FreeIpaModifyTagsService underTest;

    @Test
    void testModifyUserDefinedTags() {
        Stack stack = mock(Stack.class);
        String environmentCrn = "environmentCrn";
        String accountId = "accountId";
        Map<String, String> userDefinedTags = Map.of("owner", "john doe");

        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(environmentCrn, accountId)).thenReturn(stack);

        underTest.modifyUserDefinedTags(environmentCrn, userDefinedTags, accountId);

        verify(stackUpdater).updateUserDefinedTags(stack, userDefinedTags);
    }
}