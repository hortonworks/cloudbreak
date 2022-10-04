package com.sequenceiq.cloudbreak.service.dr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.datalakemetrics.datasizes.DatalakeDataSizesV4Response;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.datalakemetrics.GetDatalakeDataSizesService;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;

@ExtendWith(MockitoExtension.class)
public class GetDatalakeDataSizesServiceTest {
    private static final Long STACK_ID = 0L;

    private static final Long HIVE_SIZE = 12153503L;

    private static final Long RANGER_SIZE = 18854559L;

    private static final Long PROFILER_AGENT_SIZE = 20000L;

    private static final Long PROFILER_METRIC_SIZE = 10000L;

    private static final Long TOTAL_DB_SIZE = HIVE_SIZE + RANGER_SIZE + PROFILER_AGENT_SIZE + PROFILER_METRIC_SIZE;

    private static final Long ATLAS_ENTITY_SIZE = 408L;

    private static final Long ATLAS_JANUS_SIZE = 954200L;

    private static final Long EDGE_SIZE = 69L;

    private static final Long FULLTEXT_SIZE = 69L;

    private static final Long RANGER_AUDITS_SIZE = 1663331L;

    private static final Long VERTEX_SIZE = 496952L;

    private static final String TEST_DATA_SIZES_RESPONSE = '{' +
            "\"database\":{" +
                "\"hive\":" + HIVE_SIZE + ',' +
                "\"ranger\":" + RANGER_SIZE + ',' +
                "\"profiler_agent\":" + PROFILER_AGENT_SIZE + ',' +
                "\"profiler_metric\":" + PROFILER_METRIC_SIZE +
            "}," +
            "\"hbase\":{" +
                "\"atlas_entity_audit_events\":" + ATLAS_ENTITY_SIZE + ',' +
                "\"atlas_janus\":" + ATLAS_JANUS_SIZE +
            "}," +
            "\"solr\":{" +
                "\"edge_index\":" + EDGE_SIZE + ',' +
                "\"fulltext_index\":" + FULLTEXT_SIZE + ',' +
                "\"ranger_audits\":" + RANGER_AUDITS_SIZE + ',' +
                "\"vertex_index\":" + VERTEX_SIZE +
            '}' +
        '}';

    @Mock
    private StackStatusService stackStatusService;

    @Mock
    private Clock clock;

    @Mock
    private StackUpdater stackUpdater;

    @InjectMocks
    private GetDatalakeDataSizesService getDatalakeDataSizesService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(clock.getCurrentTimeMillis()).thenReturn(100000000L);
    }

    @Test
    public void testSuccessfulParse() {
        when(stackStatusService.findAllStackStatusesById(eq(STACK_ID), anyLong())).thenReturn(getStackStatuses());

        DatalakeDataSizesV4Response out = getDatalakeDataSizesService.getDatalakeDataSizes(getStack());
        assertEquals(out.getDatabaseSizeInBytes(), TOTAL_DB_SIZE);
        assertEquals(out.getHbaseAtlasEntityAuditEventsTableSizeInBytes(), ATLAS_ENTITY_SIZE);
        assertEquals(out.getHbaseAtlasJanusTableSizeInBytes(), ATLAS_JANUS_SIZE);
        assertEquals(out.getSolrEdgeIndexCollectionSizeInBytes(), EDGE_SIZE);
        assertEquals(out.getSolrFullextIndexCollectionSizeInBytes(), FULLTEXT_SIZE);
        assertEquals(out.getSolrRangerAuditsCollectionSizeInBytes(), RANGER_AUDITS_SIZE);
        assertEquals(out.getSolrVertexIndexCollectionSizeInBytes(), VERTEX_SIZE);
    }

    @Test
    public void testStatusNotFound() {
        when(stackStatusService.findAllStackStatusesById(eq(STACK_ID), anyLong())).thenReturn(List.of());
        assertThrows(NotFoundException.class, () -> getDatalakeDataSizesService.getDatalakeDataSizes(getStack()));
    }

    @Test
    public void testInvalidJson() {
        List<StackStatus> statuses = getStackStatuses();
        statuses.get(0).setStatusReason("{}");
        when(stackStatusService.findAllStackStatusesById(eq(STACK_ID), anyLong())).thenReturn(statuses);
        assertThrows(CloudbreakServiceException.class, () -> getDatalakeDataSizesService.getDatalakeDataSizes(getStack()));
    }

    private Stack getStack() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        return stack;
    }

    private List<StackStatus> getStackStatuses() {
        StackStatus status = new StackStatus();
        status.setDetailedStackStatus(DetailedStackStatus.DETERMINE_DATALAKE_DATA_SIZES_FINISHED);
        status.setStatusReason(TEST_DATA_SIZES_RESPONSE);
        return List.of(status);
    }
}
