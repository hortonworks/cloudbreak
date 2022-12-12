package com.sequenceiq.cloudbreak.datalakedr.converter;

import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.datalakedr.datalakeDRProto.DatalakeDataInfoObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Service
public class DatalakeDataInfoJsonToObjectConverter {
    public DatalakeDataInfoObject convert(String operationId, String inputJSON) {
        try {
            JsonObject json = JsonParser.parseString(inputJSON).getAsJsonObject();
            JsonObject databaseJSON = json.getAsJsonObject("database");
            JsonObject hbaseJSON = json.getAsJsonObject("hbase");
            JsonObject solrJSON = json.getAsJsonObject("solr");
            JsonElement backupSpaceJSON = json.get("freeSpace");

            return DatalakeDataInfoObject.newBuilder()
                    .setOperationId(operationId)
                    .setDatabaseSizeInBytes(getTotalDatabaseSize(databaseJSON))
                    .setDatabaseBackupNodeFreeSpaceInBytes(backupSpaceJSON.getAsLong())
                    .setHbaseAtlasEntityAuditEventsTableSizeInBytes(hbaseJSON.get("atlas_entity_audit_events").getAsLong())
                    .setHbaseAtlasJanusTableSizeInBytes(hbaseJSON.get("atlas_janus").getAsLong())
                    .setSolrVertexIndexCollectionSizeInBytes(solrJSON.get("vertex_index").getAsLong())
                    .setSolrFulltextIndexCollectionSizeInBytes(solrJSON.get("fulltext_index").getAsLong())
                    .setSolrEdgeIndexCollectionSizeInBytes(solrJSON.get("edge_index").getAsLong())
                    .setSolrRangerAuditsCollectionSizeInBytes(solrJSON.get("ranger_audits").getAsLong())
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
}
