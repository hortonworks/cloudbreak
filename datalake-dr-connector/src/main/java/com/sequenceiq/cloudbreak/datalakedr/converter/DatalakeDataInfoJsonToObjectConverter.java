package com.sequenceiq.cloudbreak.datalakedr.converter;

import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.DatalakeDataInfoObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Service
public class DatalakeDataInfoJsonToObjectConverter {

    public static final int BYTES_IN_FILESYSTEM_BLOCK = 1024;

    public DatalakeDataInfoObject convert(String operationId, String inputJSON) {
        try {
            JsonObject json = JsonParser.parseString(inputJSON).getAsJsonObject();
            JsonObject databaseJSON = json.getAsJsonObject("database");
            JsonObject hbaseJSON = json.getAsJsonObject("hbase");
            JsonObject solrJSON = json.getAsJsonObject("solr");
            JsonElement backupSpaceJSON = json.get("freeSpace");
            long databaseBackupNodeFreeSpaceInBytes = backupSpaceJSON != null ?
                    (backupSpaceJSON.getAsLong() < 0 ?
                        backupSpaceJSON.getAsLong() :
                        backupSpaceJSON.getAsLong() * BYTES_IN_FILESYSTEM_BLOCK
                    ) : 0;
            return DatalakeDataInfoObject.newBuilder()
                    .setOperationId(operationId)
                    .setDatabaseSizeInBytes(databaseJSON != null ? getTotalDatabaseSize(databaseJSON) : 0)
                    .setDatabaseBackupNodeFreeSpaceInBytes(databaseBackupNodeFreeSpaceInBytes < 0 ? 0 : databaseBackupNodeFreeSpaceInBytes)
                    .setSigneddatabaseBackupNodeFreeSpaceInBytes(databaseBackupNodeFreeSpaceInBytes)
                    .setHbaseAtlasEntityAuditEventsTableSizeInBytes(getLongFromJson(hbaseJSON, "atlas_entity_audit_events", 0))
                    .setHbaseAtlasJanusTableSizeInBytes(getLongFromJson(hbaseJSON, "atlas_janus", 0))
                    .setSolrVertexIndexCollectionSizeInBytes(getLongFromJson(solrJSON, "vertex_index", 0))
                    .setSolrFulltextIndexCollectionSizeInBytes(getLongFromJson(solrJSON, "fulltext_index", 0))
                    .setSolrEdgeIndexCollectionSizeInBytes(getLongFromJson(solrJSON, "edge_index", 0))
                    .setSolrRangerAuditsCollectionSizeInBytes(getLongFromJson(solrJSON, "ranger_audits", 0))
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse the data info object JSON '" + inputJSON + '\'', ex);
        }
    }

    private Long getTotalDatabaseSize(JsonObject databaseJSON) {
        return databaseJSON.entrySet().stream()
                .map(entry -> entry.getValue().getAsLong())
                .reduce(0L, Long::sum);
    }

    private long getLongFromJson(JsonObject json, String key, long defaultValue) {
        return json.has(key) ? json.get(key).getAsLong() : defaultValue;
    }
}
