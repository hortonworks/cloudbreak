package com.sequenceiq.cloudbreak.audit.converter.auditeventname.rest;

import static com.sequenceiq.cloudbreak.audit.converter.EventDataExtractor.CLUSTER_CRN;
import static com.sequenceiq.cloudbreak.audit.converter.EventDataExtractor.CLUSTER_NAME;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;

@Component
public class RestCommonService {

    public void addClusterCrnAndNameIfPresent(StructuredRestCallEvent structuredEvent, Map<String, Object> params) {
        String resourceCrn = structuredEvent.getOperation().getResourceCrn();
        RestRequestDetails restRequest = structuredEvent.getRestCall().getRestRequest();
        Json requestJson = new Json(restRequest.getBody());
        if (StringUtils.isEmpty(resourceCrn)) {
            resourceCrn = getValueFromJson(requestJson, "resourceCrn");
        }
        String name = null;

        String responseBody = structuredEvent.getRestCall().getRestResponse().getBody();
        if (StringUtils.isNotEmpty(responseBody)) {
            Json responseJson = new Json(responseBody);
            if (StringUtils.isEmpty(resourceCrn)) {
                resourceCrn = getValueFromJson(responseJson, "resourceCrn");
            }
            name = getValueFromJson(responseJson, "name");
        }

        if (StringUtils.isEmpty(name)) {
            name = getValueFromJson(requestJson, "name");
        }

        if (StringUtils.isEmpty(name)) {
            String method = restRequest.getMethod();
            if ("DELETE".equals(method)) {
                List<String> names = requestJson.getValue("names");
                name = String.join(",", names);
            }
        }

        checkNameOrCrnProvided(restRequest, resourceCrn, name);

        if (StringUtils.isNotEmpty(name)) {
            params.put(CLUSTER_NAME, name);
        }

        if (StringUtils.isNotEmpty(resourceCrn)) {
            params.put(CLUSTER_CRN, resourceCrn);
        }
    }

    private String getValueFromJson(Json responseJson, String resourceCrn) {
        return responseJson.getValue(resourceCrn);
    }

    private void checkNameOrCrnProvided(RestRequestDetails restRequest, String resourceCrn, String name) {
        if (StringUtils.isEmpty(name) && StringUtils.isEmpty(resourceCrn)) {
            throw new UnsupportedOperationException(String.format("Cannot determine the resource crn or name, so we does not support for auditing for method: "
                    + "%s, uri: %s, body: %s", restRequest.getMethod(), restRequest.getRequestUri(), restRequest.getBody()));
        }
    }
}
