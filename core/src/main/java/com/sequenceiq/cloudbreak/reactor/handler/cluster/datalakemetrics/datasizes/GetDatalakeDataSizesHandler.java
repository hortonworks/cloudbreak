package com.sequenceiq.cloudbreak.reactor.handler.cluster.datalakemetrics.datasizes;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.metrics.datasizes.DetermineDatalakeDataSizesEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.datalakemetrics.datasizes.DetermineDatalakeDataSizesSuccessEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.datalakemetrics.datasizes.GetDatalakeDataSizesRequest;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class GetDatalakeDataSizesHandler extends ExceptionCatcherEventHandler<GetDatalakeDataSizesRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetDatalakeDataSizesHandler.class);

    private static final String GET_DATABASE_SIZES_STATE = "datalake_metrics.get_database_sizes.get_database_sizes";

    private static final String GET_SOLR_HBASE_DATA_SIZES_STATE = "datalake_metrics.get_solr_hbase_data_sizes.get_solr_hbase_data_sizes";

    private static final String CORE_INSTANCE_GROUP_NAME = "core";

    private static final String SALT_RESPONSE_STATE_FIELD = "__sls__";

    private static final String SALT_RESPONSE_SUCCESS_FIELD = "result";

    private static final String SALT_RESPONSE_OUTPUT_FIELD = "changes";

    private static final String SALT_RESPONSE_RESULTS_FIELD = "stdout";

    private static final String[] EXPECTED_SIZE_SOURCES = {"database", "hbase", "solr"};

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(GetDatalakeDataSizesRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long stackId, Exception ex, Event<GetDatalakeDataSizesRequest> request) {
        LOGGER.error("Failed to get the datalake data sizes for stack with ID: '" + stackId.toString() + '\'', ex);
        return new StackFailureEvent(DetermineDatalakeDataSizesEvent.DETERMINE_DATALAKE_DATA_SIZES_FAILURE_EVENT.event(), stackId, ex);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<GetDatalakeDataSizesRequest> event) {
        GetDatalakeDataSizesRequest request = event.getData();
        Long stackId = request.getResourceId();
        StackDto stackDto = stackDtoService.getById(stackId);
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stackDto);

        try {
            String databaseSizes = runSaltStateAndGetResult(
                    primaryGatewayConfig, GET_DATABASE_SIZES_STATE, primaryGatewayConfig.getHostname(), stackId
            );
            String solrHBaseSizes = runSaltStateAndGetResult(
                    primaryGatewayConfig, GET_SOLR_HBASE_DATA_SIZES_STATE, getTargetHostName(stackDto, primaryGatewayConfig),
                    stackId
            );
            String result = '{' + databaseSizes + ',' + solrHBaseSizes + '}';

            checkDataSizesResultValid(result);
            return new DetermineDatalakeDataSizesSuccessEvent(stackId, result);
        } catch (Exception ex) {
            LOGGER.error("Failed to get datalake data sizes via Salt.", ex);
            return new StackFailureEvent(DetermineDatalakeDataSizesEvent.DETERMINE_DATALAKE_DATA_SIZES_FAILURE_EVENT.event(), stackId, ex);
        }
    }

    private String runSaltStateAndGetResult(GatewayConfig primaryGatewayConfig, String state, String targetHostName, Long stackId)
            throws CloudbreakOrchestratorFailedException {
        LOGGER.info("Attempting to run Salt state {} for stack with ID '{}'", state, stackId);

        OrchestratorStateParams stateParams = new OrchestratorStateParams();
        stateParams.setPrimaryGatewayConfig(primaryGatewayConfig);
        stateParams.setState(state);
        stateParams.setTargetHostNames(Set.of(targetHostName));

        String result = saltResultToDataSizesResult(hostOrchestrator.applyOrchestratorState(stateParams), state);
        LOGGER.info("Finished running Salt state {} for stack with ID '{}'", state, stackId);
        return result;
    }

    private String getTargetHostName(StackDto stackDto, GatewayConfig primaryGatewayConfig) {
        Optional<String> coreNodeHostName = stackDto.getAllAvailableInstances().stream()
                .filter(im -> CORE_INSTANCE_GROUP_NAME.equals(im.getInstanceGroupName()) &&
                        InstanceMetadataType.CORE.equals(im.getInstanceMetadataType()))
                .findFirst()
                .map(InstanceMetadataView::getDiscoveryFQDN);
        return coreNodeHostName.orElse(primaryGatewayConfig.getHostname());
    }

    private String saltResultToDataSizesResult(List<Map<String, JsonNode>> saltResponse, String state) {
        Optional<String> dataSizesResult = Optional.empty();
        if (!saltResponse.isEmpty()) {
            dataSizesResult = saltResponse.get(0).values().stream().findFirst()
                .map(responses -> getDataSizesStateResponseNode(responses, state))
                .map(response -> response.get(SALT_RESPONSE_OUTPUT_FIELD))
                .map(output -> output.get(SALT_RESPONSE_RESULTS_FIELD))
                .map(JsonNode::asText);
        }

        return dataSizesResult.orElseThrow(() -> {
            LOGGER.error("The data sizes result from Salt is malformed and cannot be interpreted.");
            return new RuntimeException("Failed to interpret data sizes result!");
        });
    }

    private JsonNode getDataSizesStateResponseNode(JsonNode saltStateResponses, String state) {
        Iterator<JsonNode> responses = saltStateResponses.elements();
        JsonNode curResponse;
        while (responses.hasNext()) {
            curResponse = responses.next();
            if (curResponse.get(SALT_RESPONSE_STATE_FIELD).asText().replace("\"", "").equals(state) &&
                    curResponse.get(SALT_RESPONSE_SUCCESS_FIELD).asBoolean()) {
                return curResponse;
            }
        }
        LOGGER.error("None of the Salt state responses is a successful response to the get data sizes command.");
        return null;
    }

    private void checkDataSizesResultValid(String result) {
        if (!Arrays.stream(EXPECTED_SIZE_SOURCES).allMatch(result::contains)) {
            LOGGER.error("The data sizes result is not valid! Result: {}", result);
            throw new RuntimeException("The data sizes result is not valid! Result: " + result);
        }
    }
}
