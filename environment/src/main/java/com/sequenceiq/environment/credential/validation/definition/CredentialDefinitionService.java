package com.sequenceiq.environment.credential.validation.definition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.environment.credential.service.ResourceDefinitionService;
import com.sequenceiq.environment.exception.MissingParameterException;

@Service
public class CredentialDefinitionService {

    private static final String SELECTOR = "selector";

    private static final String RESOURCE_TYPE = "credential";

    @Inject
    private ResourceDefinitionService definitionService;

    public void checkPropertiesRemoveSensitives(Platform cloudPlatform, Json json) {
        processValues(getDefinition(cloudPlatform), json);
    }

    private Definition getDefinition(Platform cloudPlatform) {
        String json = definitionService.getResourceDefinition(cloudPlatform.value(), RESOURCE_TYPE);
        try {
            return JsonUtil.readValue(json, Definition.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void processValues(Definition definition, Json json) {
        processValues(json, definition.getDefaultValues());
        String selector = json.getString(SELECTOR);
        if (selector != null) {
            processValues(json, collectSelectorValues(definition, selector));
        }
    }

    private Iterable<Value> collectSelectorValues(Definition definition, String selectorName) {
        List<Value> values = new ArrayList<>();
        List<Selector> selectors = definition.getSelectors();
        String currentSelector = selectorName;
        Selector element;
        while ((element = findSelector(selectors, currentSelector)) != null) {
            values.addAll(element.getValues());
            currentSelector = element.getParent();
        }
        return values;
    }

    private Selector findSelector(Iterable<Selector> selectors, String selector) {
        for (Selector s : selectors) {
            if (s.getName().equals(selector)) {
                return s;
            }
        }
        return null;
    }

    private void processValues(Json json, Iterable<Value> values) {
        Set<String> flatValues = json.flatPaths();
        for (Value value : values) {
            String key = value.getName();
            String property = getProperty(json, key, isOptional(value));
            if (!isSensitve(value) && property != null) {
                flatValues.remove(value.getName());
            }
        }
        flatValues.forEach(json::remove);
    }

    private boolean isSensitve(Value value) {
        Boolean sensitive = value.getSensitive();
        return isNotNull(sensitive) && sensitive;
    }

    private boolean isOptional(Value value) {
        Boolean optional = value.getOptional();
        return isNotNull(optional) && optional;
    }

    private boolean isNotNull(Object object) {
        return null != object;
    }

    private String getProperty(Json json, String key, boolean optional) {
        String value = json.getString(key);
        if (value == null && !optional) {
            throw new MissingParameterException(String.format("Missing '%s' property!", key));
        }
        return value == null ? null : value;
    }

}
