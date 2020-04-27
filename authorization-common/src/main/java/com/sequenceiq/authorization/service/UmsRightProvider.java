package com.sequenceiq.authorization.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.ws.rs.InternalServerErrorException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceActionModel;
import com.sequenceiq.authorization.resource.AuthorizationResourceActionType;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class UmsRightProvider {

    private final Map<AuthorizationResourceAction, AuthorizationResourceActionModel> actions = new HashMap<>();

    @PostConstruct
    public void init() {
        readActions();
    }

    public AuthorizationResourceType getResourceType(AuthorizationResourceAction action) {
        return getActionModel(action).getResourceType();
    }

    public AuthorizationResourceActionType getActionType(AuthorizationResourceAction action) {
        return getActionModel(action).getActionType();
    }

    public String getRight(AuthorizationResourceAction action) {
        return getActionModel(action).getRight();
    }

    public Optional<AuthorizationResourceAction> getByName(String name) {
        return actions.entrySet().stream()
                .filter(entry -> StringUtils.equals(entry.getValue().getRight(), name))
                .map(Map.Entry::getKey)
                .findAny();
    }

    private AuthorizationResourceActionModel getActionModel(AuthorizationResourceAction action) {
        if (actions.containsKey(action)) {
            return actions.get(action);
        }
        throw new InternalServerErrorException(String
                .format("Action %s is not present in actions.json, thus we cannot provide information about it.", action));
    }

    void readActions() {
        try {
            String actionsJson = FileReaderUtils.readFileFromClasspath("actions.json");
            JsonNode tree = JsonUtil.readTree(actionsJson);
            Arrays.stream(AuthorizationResourceAction.values()).forEach(action -> {
                AuthorizationResourceActionModel actionModel = new ObjectMapper().convertValue(tree.get(action.name()), AuthorizationResourceActionModel.class);
                if (actionModel != null) {
                    actions.put(action, actionModel);
                }
            });
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Cannot initialize actions for permission check.", e);
        }
    }
}
