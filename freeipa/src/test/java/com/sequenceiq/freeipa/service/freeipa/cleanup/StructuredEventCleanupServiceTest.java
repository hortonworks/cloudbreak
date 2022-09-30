package com.sequenceiq.freeipa.service.freeipa.cleanup;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.structuredevent.repository.CDPStructuredEventRepository;
import com.sequenceiq.cloudbreak.util.TimeUtil;
import com.sequenceiq.freeipa.service.stack.StackService;

@RunWith(MockitoJUnitRunner.class)
public class StructuredEventCleanupServiceTest {

    private static final long STACK_ID = 1L;

    @InjectMocks
    private StructuredEventCleanupService underTest;

    @Mock
    private StackService stackService;

    @Mock
    private TimeUtil mockTimeUtil;

    @Mock
    private CDPStructuredEventRepository mockCDPStructuredEventRepository;

    @Test
    public void testCleanUpStructuredEventsWhenNoResourceCrnFoundForStackIdThenNoCleanUpHappens() {
        underTest.cleanUpStructuredEvents(STACK_ID);

        verifyNoMoreInteractions(mockCDPStructuredEventRepository);
    }

    @Test
    public void testCleanUpStructuredEventsWhenResourceCrnExistsThenTransactionHappens() {
        String accountId = "accountId";
        when(stackService.findAccountById(STACK_ID)).thenReturn(Optional.of(accountId));
        when(mockTimeUtil.getTimestampThatMonthsBeforeNow(3)).thenReturn(1L);

        underTest.cleanUpStructuredEvents(STACK_ID);

        verify(mockCDPStructuredEventRepository, times(1)).deleteByAccountIdOlderThan(accountId, 1L);
    }

}