package com.sequenceiq.cloudbreak.service.datalakemetrics;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.datalakemetrics.datasizes.DatalakeDataSizesV4Response;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;

@Service
public class GetDatalakeDataSizesService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetDatalakeDataSizesService.class);

    private static final int MAX_STATUS_AGE_IN_MILLIS = 5 * 60 * 1000;

    @Inject
    private StackStatusService stackStatusService;

    @Inject
    private Clock clock;

    @Inject
    private StackUpdater stackUpdater;

    public DatalakeDataSizesV4Response getDatalakeDataSizes(Stack stack) {
        List<StackStatus> lastStatuses = stackStatusService.findAllStackStatusesById(
                stack.getId(), clock.getCurrentTimeMillis() - MAX_STATUS_AGE_IN_MILLIS
        );
        for (StackStatus status : lastStatuses) {
            if (status.getDetailedStackStatus().equals(DetailedStackStatus.DETERMINE_DATALAKE_DATA_SIZES_FINISHED)) {
                LOGGER.info("Found datalake data sizes result in list of stack statuses! Removing result from status reason.");
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.AVAILABLE);
                return parseResult(status.getStatusReason());
            }
        }
        LOGGER.error("Unable to find datalake data sizes result.");
        throw new NotFoundException("Failed to find datalake data sizes result in stack statuses.");
    }

    private DatalakeDataSizesV4Response parseResult(String dataSizesResponse) {
        DatalakeDataSizesV4Response response = new DatalakeDataSizesV4Response();

        try {
            JsonObject json = JsonParser.parseString(dataSizesResponse).getAsJsonObject();
            addDatabaseSize(response, json);
            addHBaseTableSizes(response, json);
            addSolrCollectionSizes(response, json);
            return response;
        } catch (Exception ex) {
            throw new CloudbreakServiceException("Failed to parse the data sizes result '" + dataSizesResponse + '\'', ex);
        }
    }

    private void addDatabaseSize(DatalakeDataSizesV4Response response, JsonObject result) {
        JsonObject databaseJSON = result.getAsJsonObject("database");
        response.setDatabaseSizeInBytes(databaseJSON.entrySet().stream()
            .map(entry -> entry.getValue().getAsLong())
            .reduce(0L, Long::sum));
    }

    private void addHBaseTableSizes(DatalakeDataSizesV4Response response, JsonObject result) {
        JsonObject hbaseJSON = result.getAsJsonObject("hbase");
        response.setHbaseAtlasEntityAuditEventsTableSizeInBytes(hbaseJSON.get("atlas_entity_audit_events").getAsLong());
        response.setHbaseAtlasJanusTableSizeInBytes(hbaseJSON.get("atlas_janus").getAsLong());
    }

    private void addSolrCollectionSizes(DatalakeDataSizesV4Response response, JsonObject result) {
        JsonObject solrJSON = result.getAsJsonObject("solr");
        response.setSolrVertexIndexCollectionSizeInBytes(solrJSON.get("vertex_index").getAsLong());
        response.setSolrFullextIndexCollectionSizeInBytes(solrJSON.get("fulltext_index").getAsLong());
        response.setSolrEdgeIndexCollectionSizeInBytes(solrJSON.get("edge_index").getAsLong());
        response.setSolrRangerAuditsCollectionSizeInBytes(solrJSON.get("ranger_audits").getAsLong());
    }
}
