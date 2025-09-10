package com.sequenceiq.cloudbreak.structuredevent.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;

@Component
public class LegacyRestCommonService {

    public static final String NAME_PATH = "name";

    public static final String NAMES_PATH = "names";

    public static final String RESOURCE_CRN_PATH = "resourceCrn";

    public static final String CRNS_PATH = "crns";

    public static final String CLUSTER_CRN = "clusterCrn";

    public static final String CLUSTER_NAME = "clusterName";

    public Map<String, Object> addClusterCrnAndNameIfPresent(StructuredRestCallEvent structuredEvent) {
        Map<String, Object> params = new HashMap<>();
        RestRequestDetails restRequest = structuredEvent.getRestCall().getRestRequest();
        Json requestJson = getJson(restRequest.getBody());
        Json responseJson = getJson(structuredEvent.getRestCall().getRestResponse().getBody());
        String resourceCrn = getCrn(requestJson, responseJson, restRequest, structuredEvent.getOperation());
        String name = getName(requestJson, responseJson, restRequest, structuredEvent.getOperation());

        checkNameOrCrnProvided(restRequest, resourceCrn, name);

        if (StringUtils.isNotEmpty(name)) {
            params.put(CLUSTER_NAME, name);
        }

        if (StringUtils.isNotEmpty(resourceCrn)) {
            params.put(CLUSTER_CRN, resourceCrn);
        }
        return params;
    }

    private String getName(Json requestJson, Json responseJson, RestRequestDetails request, OperationDetails operationDetails) {
        String name = operationDetails.getResourceName();
        if (StringUtils.isEmpty(name)) {
            name = getResourceId(requestJson, responseJson, request, NAME_PATH, NAMES_PATH);
        }
        return name;
    }

    private String getCrn(Json requestJson, Json responseJson, RestRequestDetails request, OperationDetails operationDetails) {
        String resourceCrn = operationDetails.getResourceCrn();
        if (StringUtils.isEmpty(resourceCrn)) {
            resourceCrn = getResourceId(requestJson, responseJson, request, RESOURCE_CRN_PATH, CRNS_PATH);
        }
        return resourceCrn;
    }

    private String getResourceId(Json requestJson, Json responseJson, RestRequestDetails request, String path,
            String pluralPath) {
        String id = null;
        if (requestJson != null) {
            id = getValueFromJson(requestJson, path);
            if (StringUtils.isEmpty(id)) {
                id = getListValue(request, requestJson, pluralPath);
            }
        }
        if (responseJson != null) {
            if (StringUtils.isEmpty(id)) {
                id = getValueFromJson(responseJson, path);
            }
        }
        return id;
    }

    private String getListValue(RestRequestDetails restRequest, Json requestJson, String path) {
        String method = restRequest.getMethod();
        if ("DELETE".equals(method)) {
            List<String> values = JsonUtil.readValue(requestJson.getJsonNode(path), List.class);
            if (CollectionUtils.isNotEmpty(values)) {
                return String.join(",", values);
            }
        }
        return null;
    }

    private Json getJson(String body) {
        if (body != null && StringUtils.isNotEmpty(body.trim())) {
            if (!JsonUtil.isValid(body)) {
                throw new IllegalArgumentException("Invalid json: " + AnonymizerUtil.anonymize(body));
            }
            return new Json(body);
        }
        return null;
    }

    private String getValueFromJson(Json responseJson, String resourceCrn) {
        return responseJson.getString(resourceCrn);
    }

    private void checkNameOrCrnProvided(RestRequestDetails restRequest, String resourceCrn, String name) {
        if (StringUtils.isEmpty(name) && StringUtils.isEmpty(resourceCrn)) {
            throw new UnsupportedOperationException(String.format("Cannot determine the resource crn or name, so we does not support for auditing for method: "
                    + "%s, uri: %s, body: %s", restRequest.getMethod(), restRequest.getRequestUri(), restRequest.getBody()));
        }
    }
}
