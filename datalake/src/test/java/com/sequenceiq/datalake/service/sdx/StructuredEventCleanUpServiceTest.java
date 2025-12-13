package com.sequenceiq.datalake.service.sdx;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.structuredevent.service.db.CDPStructuredEventDBService;
import com.sequenceiq.cloudbreak.util.TimeUtil;

@ExtendWith(MockitoExtension.class)
class StructuredEventCleanUpServiceTest {

    private static final Long TEST_SDX_ID = 1L;

    private static final Optional<String> TEST_ACCOUNT_ID = Optional.of("something");

    @Mock
    private SdxService mockSdxService;

    @Mock
    private TimeUtil mockTimeUtil;

    @Mock
    private CDPStructuredEventDBService mockCDPStructuredEventDBService;

    @InjectMocks
    private StructuredEventCleanUpService underTest;

    @Test
    void testCleanUpWhenSdxServiceDoesNotFindAccountIdForIdThenNothingHappens() {
        when(mockSdxService.findResourceCrnById(TEST_SDX_ID)).thenReturn(Optional.empty());

        underTest.cleanUpStructuredEvents(TEST_SDX_ID);

        verify(mockSdxService, times(1)).findResourceCrnById(TEST_SDX_ID);
        verifyNoMoreInteractions(mockCDPStructuredEventDBService);
        verifyNoMoreInteractions(mockSdxService);
    }

    @Test
    void testCleanUpStructuredEventsWhenSdxServiceCanFindResourceCrnForIdThenTransactionHappens() throws TransactionExecutionException {
        when(mockSdxService.findResourceCrnById(TEST_SDX_ID)).thenReturn(TEST_ACCOUNT_ID);
        long timestamp = 1234567;
        when(mockTimeUtil.getTimestampThatMonthsBeforeNow(3)).thenReturn(timestamp);

        underTest.cleanUpStructuredEvents(TEST_SDX_ID);

        verify(mockCDPStructuredEventDBService, times(1)).deleteStructuredEventByResourceCrnThatIsOlderThan(TEST_ACCOUNT_ID.get(), timestamp);
        verify(mockTimeUtil, times(1)).getTimestampThatMonthsBeforeNow(3);
        verify(mockSdxService, times(1)).findResourceCrnById(TEST_SDX_ID);
        verifyNoMoreInteractions(mockCDPStructuredEventDBService);
        verifyNoMoreInteractions(mockSdxService);
        verifyNoMoreInteractions(mockTimeUtil);
    }

}