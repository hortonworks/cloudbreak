package com.sequenceiq.cloudbreak.telemetry.converter;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.common.api.diagnostics.DiagnosticsCollectionStatus;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;

@Component
public class FlowPayloadToDiagnosticDetailsConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowPayloadToDiagnosticDetailsConverter.class);

    private static final int MAX_PERCENT = 100;

    public Map<String, Object> convert(String payload) {
        Map<String, Object> properties = new HashMap<>();
        try {
            JsonNode payloadNode = JsonUtil.readTree(payload);
            if (payloadNode.isObject() && payloadNode.has("parameters")) {
                JsonNode parametersNode = payloadNode.get("parameters");
                if (!parametersNode.isEmpty()) {
                    fillStrFieldIfExists(parametersNode, "accountId", "accountId", properties);
                    fillStrFieldIfExists(parametersNode, "issue", "case", properties);
                    fillStrFieldIfExists(parametersNode, "uuid", "uuid", properties);
                    fillStrFieldIfExists(parametersNode, "description", "description", properties);
                    fillStrFieldIfExists(parametersNode, "clusterType", "clusterType", properties);
                    fillStrFieldIfExists(parametersNode, "clusterVersion", "clusterVersion", properties);
                    fillStrFieldIfExists(parametersNode, "statusReason", "statusReason", properties);
                    fillDestinationData(properties, parametersNode);
                }
            }
            fillStrFieldIfExists(payloadNode, "resourceCrn", "resourceCrn", properties);
        } catch (IOException e) {
            LOGGER.warn("Flow payload cannot be read for diagnostics.", e);
        }
        return properties;
    }

    public int calculateProgressPercentage(boolean finalized, boolean serviceStatusFailed,
            Supplier<Integer> calculateProgressFunction) {
        int result = 0;
        if (finalized || serviceStatusFailed) {
            result = MAX_PERCENT;
        } else {
            result = calculateProgressFunction.get();
        }
        return result;
    }

    public DiagnosticsCollectionStatus calculateStatus(String currentState, String finishedState, String failedState,
            String failedHandledEvent, String nextEvent, boolean finalized, boolean serviceStatusFailed) {
        DiagnosticsCollectionStatus status = DiagnosticsCollectionStatus.IN_PROGRESS;
        if (serviceStatusFailed) {
            status = DiagnosticsCollectionStatus.FAILED;
        } else if (finalized) {
            if (finishedState.equals(currentState)) {
                status = DiagnosticsCollectionStatus.FINISHED;
            } else if (failedState.equals(currentState)
                    || failedHandledEvent.equals(nextEvent)) {
                status = DiagnosticsCollectionStatus.FAILED;
            } else {
                status = DiagnosticsCollectionStatus.CANCELLED;
            }
        }
        return status;
    }

    private void fillDestinationData(Map<String, Object> properties, JsonNode parametersNode) {
        String destination = "";
        if (parametersNode.has("destination") && parametersNode.get("destination").has("name")) {
            destination = parametersNode.get("destination").get("name").textValue();
            properties.put("destination", destination);
        }
        if (DiagnosticsDestination.CLOUD_STORAGE.name().equals(destination)) {
            if (parametersNode.has("cloudStorageDiagnosticsParameters")) {
                fillCloudStorageOutput(parametersNode.get("cloudStorageDiagnosticsParameters"), properties);
            }
        }
        if (DiagnosticsDestination.SUPPORT.name().equals(destination)) {
            fillStrFieldIfExists(parametersNode, "dbusUrl", "databusEndpoint", properties);
        }
    }

    private void fillCloudStorageOutput(JsonNode cloudStorageDiagParams, Map<String, Object> properties) {
        if (cloudStorageDiagParams.has("s3Location")) {
            properties.put("output", "s3://"
                    + Paths.get(cloudStorageDiagParams.get("s3Bucket").textValue(), cloudStorageDiagParams.get("s3Location").textValue()));
        } else if (cloudStorageDiagParams.has("adlsV2StorageLocation")) {
            properties.put("output", "abfs://" + Paths.get(cloudStorageDiagParams.get("adlsv2StorageContainer").textValue(),
                    cloudStorageDiagParams.get("adlsv2StorageContainer").textValue()));
        } else if (cloudStorageDiagParams.has("gcsLocation")) {
            properties.put("output", "gcs://"
                    + Paths.get(cloudStorageDiagParams.get("bucket").textValue(), cloudStorageDiagParams.get("gcsLocation").textValue()));
        }
    }

    private void fillStrFieldIfExists(JsonNode parametersNode, String field, String name, Map<String, Object> properties) {
        if (parametersNode.has(field)) {
            properties.put(name, parametersNode.get(field).textValue());
        }
    }
}
