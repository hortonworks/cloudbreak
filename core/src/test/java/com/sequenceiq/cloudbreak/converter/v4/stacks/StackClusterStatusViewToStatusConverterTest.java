package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.domain.projection.StackClusterStatusView;

class StackClusterStatusViewToStatusConverterTest {

    private static final String CRN = "crn:cdp:datahub:us-west-1:acc:cluster:cluster";

    private static final Long TERMINATED = 1234567890000L;

    private final StackClusterStatusViewToStatusConverter underTest = new StackClusterStatusViewToStatusConverter();

    @Test
    void testConvertMapsTerminated() {
        StackClusterStatusView source = mock(StackClusterStatusView.class);
        when(source.getId()).thenReturn(1L);
        when(source.getCrn()).thenReturn(CRN);
        when(source.getStatus()).thenReturn(Status.DELETE_COMPLETED);
        when(source.getTerminated()).thenReturn(TERMINATED);

        StackStatusV4Response result = underTest.convert(source);

        assertEquals(1L, result.getId());
        assertEquals(CRN, result.getCrn());
        assertEquals(Status.DELETE_COMPLETED, result.getStatus());
        assertEquals(TERMINATED, result.getTerminated());
    }

    @Test
    void testConvertWhenTerminatedIsNull() {
        StackClusterStatusView source = mock(StackClusterStatusView.class);
        when(source.getTerminated()).thenReturn(null);

        StackStatusV4Response result = underTest.convert(source);

        assertNull(result.getTerminated());
    }
}
