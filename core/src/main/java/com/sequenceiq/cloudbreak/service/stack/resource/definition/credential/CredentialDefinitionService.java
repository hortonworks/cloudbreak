package com.sequenceiq.cloudbreak.service.stack.resource.definition.credential;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.service.stack.resource.definition.MissingParameterException;
import com.sequenceiq.cloudbreak.service.stack.resource.definition.ResourceDefinitionService;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class CredentialDefinitionService {

    private static final String SELECTOR = "selector";
    private static final String RESOURCE_TYPE = "credential";

    @Inject
    private ResourceDefinitionService definitionService;
    @Inject
    private PBEStringCleanablePasswordEncryptor encryptor;

    public Map<String, Object> processProperties(CloudPlatform cloudPlatform, Map<String, Object> properties) throws RuntimeException {
        String json = definitionService.getResourceDefinition(cloudPlatform.name(), RESOURCE_TYPE);
        Definition definition;
        try {
            definition = JsonUtil.readValue(json, Definition.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return processValues(definition, properties);
    }

    private Map<String, Object> processValues(Definition definition, Map<String, Object> properties) throws MissingParameterException {
        Map<String, Object> processed = new HashMap<>();
        processed.putAll(processValues(properties, definition.getDefaultValues()));
        Object selector = properties.get(SELECTOR);
        if (selector != null) {
            processed.put(SELECTOR, selector);
            processed.putAll(processValues(properties, collectSelectorValues(definition, String.valueOf(selector))));
        }
        return processed;
    }

    private List<Value> collectSelectorValues(Definition definition, String selectorName) throws MissingParameterException {
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

    private Selector findSelector(List<Selector> selectors, String selector) throws MissingParameterException {
        for (Selector s : selectors) {
            if (s.getName().equals(selector)) {
                return s;
            }
        }
        return null;
    }

    private Map<String, Object> processValues(Map<String, Object> properties, List<Value> values) throws MissingParameterException {
        Map<String, Object> processed = new HashMap<>();
        for (Value value : values) {
            String key = value.getName();
            String property = getProperty(properties, key);
            if (isEncrypted(value)) {
                property = encryptor.encrypt(property);
            }
            processed.put(key, property);
        }
        return processed;
    }

    private boolean isEncrypted(Value value) {
        Boolean encrypted = value.getEncrypted();
        return isNotNull(encrypted) && encrypted;
    }

    private boolean isNotNull(Object object) {
        return null != object;
    }

    private String getProperty(Map<String, Object> properties, String key) throws MissingParameterException {
        Object value = properties.get(key);
        if (value == null) {
            throw new MissingParameterException(String.format("Missing '%s' property!", key));
        }
        return String.valueOf(value);
    }

}
