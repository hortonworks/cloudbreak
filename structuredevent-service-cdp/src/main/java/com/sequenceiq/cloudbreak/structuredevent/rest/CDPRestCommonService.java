package com.sequenceiq.cloudbreak.structuredevent.rest;

import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.ID_TYPE;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_TYPE;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;

@Component
public class CDPRestCommonService {

    public static final List<String> NAME_PATHS = List.of("name");

    public static final List<String> NAMES_PATHS = List.of("names");

    public static final List<String> RESOURCE_CRN_PATHS = List.of("crn", "resourceCrn");

    public static final List<String> CRNS_PATHS = List.of("crns");

    @Inject
    private Map<String, CustomCrnOrNameProvider> customCrnOrNameProviders;

    public Map<String, String> collectCrnAndNameIfPresent(RestCallDetails restCallDetails, CDPOperationDetails operationDetails, Map<String, String> restParams,
            String nameField, String crnField) {
        Map<String, String> params = new HashMap<>();

        RestRequestDetails restRequest = restCallDetails.getRestRequest();

        Json requestJson = getJson(restRequest.getBody());
        Json responseJson = getJson(restCallDetails.getRestResponse().getBody());

        Map<String, String> copyRestParams = new HashMap<>(restParams);
        copyRestParams.putAll(collectFromCrnOrNameProvider(restCallDetails, operationDetails, restParams, nameField, crnField));

        String resourceCrn = getCrn(requestJson, responseJson, operationDetails, copyRestParams, crnField);
        String name = getName(requestJson, responseJson, operationDetails, copyRestParams, nameField);

        checkNameOrCrnProvided(restRequest, resourceCrn, name);

        addNameAndCrnIfNotEmpty(nameField, crnField, params, resourceCrn, name);
        return params;
    }

    private void addNameAndCrnIfNotEmpty(String nameField, String crnField, Map<String, String> params, String resourceCrn, String name) {
        if (StringUtils.isNotEmpty(name)) {
            params.put(nameField, name);
        }

        if (StringUtils.isNotEmpty(resourceCrn)) {
            params.put(crnField, resourceCrn);
        }
    }

    private Map<String, String> collectFromCrnOrNameProvider(RestCallDetails restCallDetails, CDPOperationDetails operationDetails,
            Map<String, String> restParams, String nameField, String crnField) {
        CustomCrnOrNameProvider customCrnOrNameProvider = customCrnOrNameProviders.get(restParams.get(RESOURCE_TYPE) + "CustomCrnOrNameProvider");
        if (customCrnOrNameProvider != null) {
            return customCrnOrNameProvider.provide(restCallDetails, operationDetails, restParams, nameField, crnField);
        }
        return Collections.emptyMap();
    }

    private String getName(Json requestJson, Json responseJson, CDPOperationDetails operationDetails, Map<String, String> restParams, String nameField) {
        String name = restParams.get(nameField);
        if (StringUtils.isEmpty(name) && operationDetails != null) {
            name = operationDetails.getResourceName();
        }
        if (StringUtils.isEmpty(name)) {
            return getResourceId(requestJson, responseJson, NAME_PATHS, NAMES_PATHS, restParams, "name");
        }
        return name;
    }

    private String getCrn(Json requestJson, Json responseJson, CDPOperationDetails operationDetails, Map<String, String> restParams, String crnField) {
        String crn = restParams.get(crnField);
        if (StringUtils.isEmpty(crn) && operationDetails != null) {
            crn = operationDetails.getResourceCrn();
        }
        if (StringUtils.isEmpty(crn)) {
            return getResourceId(requestJson, responseJson, RESOURCE_CRN_PATHS, CRNS_PATHS, restParams, "crn");
        }
        return crn;
    }

    private String getResourceId(Json requestJson, Json responseJson, List<String> paths, List<String> pluralPaths, Map<String, String> restParams,
            String idType) {
        String id = null;
        if (requestJson != null) {
            id = getValueFromJson(requestJson, paths, restParams, idType);
            if (StringUtils.isEmpty(id)) {
                id = getValueFromJson(requestJson, pluralPaths, restParams, idType);
            }
        }
        if (responseJson != null && StringUtils.isEmpty(id)) {
            id = Optional.ofNullable(getValueFromJson(responseJson, paths, restParams, idType))
                    .orElse(getValueFromJson(responseJson, pluralPaths, restParams, idType));
        }
        return id;
    }

    private String getValueFromJson(Json json, List<String> paths, Map<String, String> restParams, String idType) {
        String values = null;
        if (json.isArray() && idType.equals(restParams.get(ID_TYPE))) {
            List<String> asList = json.asArray();
            values = String.join(",", asList);
        } else if (json.isObject() && json.getMap() != null && json.getMap().containsKey("responses")) {
            values = ((Collection<Object>) json.getMap().get("responses"))
                    .stream()
                    .map(obj -> (String) new Json(obj).getMap().get(idType))
                    .collect(Collectors.joining(","));
        } else if (json.isObject()) {
            values = getFirstPath(json, paths);
        }
        return values;
    }

    private String getFirstPath(Json json, List<String> paths) {
        String path = paths.stream().filter(p -> json.getValue(p) != null).findFirst().orElse(null);
        String value = null;
        if (path != null) {
            Object v = json.getValue(path);
            if (v instanceof Collection) {
                value = ((Collection<Object>) v).stream().map(Object::toString).collect(Collectors.joining(","));
            } else {
                value = v.toString();
            }
        }
        return value;
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

    private void checkNameOrCrnProvided(RestRequestDetails restRequest, String resourceCrn, String name) {
        if (StringUtils.isEmpty(name) && StringUtils.isEmpty(resourceCrn)) {
            throw new UnsupportedOperationException(String.format("Cannot determine the resource crn or name, so we does not support for auditing for method: "
                    + "%s, uri: %s, body: %s", restRequest.getMethod(), restRequest.getRequestUri(), restRequest.getBody()));
        }
    }
}
