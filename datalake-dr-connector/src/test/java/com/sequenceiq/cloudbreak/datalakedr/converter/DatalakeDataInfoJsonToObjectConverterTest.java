package com.sequenceiq.cloudbreak.datalakedr.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.DatalakeDataInfoObject;

@ExtendWith(MockitoExtension.class)
class DatalakeDataInfoJsonToObjectConverterTest {
    private static final String OPERATION_ID = "operation-id";

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

    private static final Long AVAILABLE_BACKUP_SIZE = 8765L;

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
            ",\"freeSpace\":" + AVAILABLE_BACKUP_SIZE +
            '}';

    private static final String TEST_DATA_SIZES_RESPONSE_MISSING_DATABASE_AND_FREESPACE = '{' +
            "\"database\":{" +
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

    private static final String TEST_DATA_SIZES_RESPONSE_MISSING_HBASE_AND_SOME_SOLR = '{' +
            "\"database\":{" +
            "\"hive\":" + HIVE_SIZE + ',' +
            "\"ranger\":" + RANGER_SIZE + ',' +
            "\"profiler_agent\":" + PROFILER_AGENT_SIZE + ',' +
            "\"profiler_metric\":" + PROFILER_METRIC_SIZE +
            "}," +
            "\"hbase\":{" +
            "}," +
            "\"solr\":{" +
            "\"edge_index\":" + EDGE_SIZE + ',' +
            "\"fulltext_index\":" + FULLTEXT_SIZE +
            '}' +
            ",\"freeSpace\":" + AVAILABLE_BACKUP_SIZE +
            '}';

    @InjectMocks
    private DatalakeDataInfoJsonToObjectConverter datalakeDataInfoJsonToObjectConverter;

    @Test
    void testSuccessfulParse() {
        DatalakeDataInfoObject out = datalakeDataInfoJsonToObjectConverter.convert(OPERATION_ID, TEST_DATA_SIZES_RESPONSE);
        assertEquals(out.getDatabaseSizeInBytes(), TOTAL_DB_SIZE);
        assertEquals(out.getHbaseAtlasEntityAuditEventsTableSizeInBytes(), ATLAS_ENTITY_SIZE);
        assertEquals(out.getHbaseAtlasJanusTableSizeInBytes(), ATLAS_JANUS_SIZE);
        assertEquals(out.getSolrEdgeIndexCollectionSizeInBytes(), EDGE_SIZE);
        assertEquals(out.getSolrFulltextIndexCollectionSizeInBytes(), FULLTEXT_SIZE);
        assertEquals(out.getSolrRangerAuditsCollectionSizeInBytes(), RANGER_AUDITS_SIZE);
        assertEquals(out.getSolrVertexIndexCollectionSizeInBytes(), VERTEX_SIZE);
        assertEquals(out.getDatabaseBackupNodeFreeSpaceInBytes(), AVAILABLE_BACKUP_SIZE * 1024);
    }

    @Test
    void testParseMissingDatabaseAndFreespace() {
        DatalakeDataInfoObject out = datalakeDataInfoJsonToObjectConverter.convert(OPERATION_ID, TEST_DATA_SIZES_RESPONSE_MISSING_DATABASE_AND_FREESPACE);
        assertEquals(out.getDatabaseSizeInBytes(), 0);
        assertEquals(out.getHbaseAtlasEntityAuditEventsTableSizeInBytes(), ATLAS_ENTITY_SIZE);
        assertEquals(out.getHbaseAtlasJanusTableSizeInBytes(), ATLAS_JANUS_SIZE);
        assertEquals(out.getSolrEdgeIndexCollectionSizeInBytes(), EDGE_SIZE);
        assertEquals(out.getSolrFulltextIndexCollectionSizeInBytes(), FULLTEXT_SIZE);
        assertEquals(out.getSolrRangerAuditsCollectionSizeInBytes(), RANGER_AUDITS_SIZE);
        assertEquals(out.getSolrVertexIndexCollectionSizeInBytes(), VERTEX_SIZE);
        assertEquals(out.getDatabaseBackupNodeFreeSpaceInBytes(), 0);
    }

    @Test
    void testParseMissingHbaseAndSomeSolr() {
        DatalakeDataInfoObject out = datalakeDataInfoJsonToObjectConverter.convert(OPERATION_ID, TEST_DATA_SIZES_RESPONSE_MISSING_HBASE_AND_SOME_SOLR);
        assertEquals(out.getDatabaseSizeInBytes(), TOTAL_DB_SIZE);
        assertEquals(out.getHbaseAtlasEntityAuditEventsTableSizeInBytes(), 0);
        assertEquals(out.getHbaseAtlasJanusTableSizeInBytes(), 0);
        assertEquals(out.getSolrEdgeIndexCollectionSizeInBytes(), EDGE_SIZE);
        assertEquals(out.getSolrFulltextIndexCollectionSizeInBytes(), FULLTEXT_SIZE);
        assertEquals(out.getSolrRangerAuditsCollectionSizeInBytes(), 0);
        assertEquals(out.getSolrVertexIndexCollectionSizeInBytes(), 0);
        assertEquals(out.getDatabaseBackupNodeFreeSpaceInBytes(), AVAILABLE_BACKUP_SIZE * 1024);
    }

    @Test
    void testInvalidJson() {
        assertThrows(IllegalStateException.class, () -> datalakeDataInfoJsonToObjectConverter.convert(OPERATION_ID, "{}"));
    }
}
