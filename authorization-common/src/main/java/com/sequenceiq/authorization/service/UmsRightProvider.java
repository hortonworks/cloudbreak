package com.sequenceiq.authorization.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class UmsRightProvider {

    private Map<String, String> legacyRights = new HashMap<>();

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @PostConstruct
    public void init() {
        readLegacyRights();
    }

    public AuthorizationResourceType getResourceType(AuthorizationResourceAction action) {
        return action.getAuthorizationResourceType();
    }

    public String getRight(AuthorizationResourceAction action, String actorCrn, String accountId) {
        if (grpcUmsClient.isAuthorizationEntitlementRegistered(actorCrn, accountId)) {
            return getNewRight(action);
        }
        return getLegacyRight(action);
    }

    public String getRight(AuthorizationResourceAction action) {
        return getRight(action, ThreadBasedUserCrnProvider.getUserCrn(), ThreadBasedUserCrnProvider.getAccountId());
    }

    public String getLegacyRight(AuthorizationResourceAction action) {
        return legacyRights.get(action.getRight());
    }

    public String getNewRight(AuthorizationResourceAction action) {
        return action.getRight();
    }

    public Optional<AuthorizationResourceAction> getByName(String name) {
        return Arrays.stream(AuthorizationResourceAction.values())
                .filter(action -> StringUtils.equals(action.getRight(), name))
                .findAny();
    }

    void readLegacyRights() {
        try {
            String legacyRightsString = FileReaderUtils.readFileFromClasspath("legacyRights.json");
            legacyRights = new ObjectMapper().readValue(legacyRightsString, Map.class);
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Cannot initialize legacy rights map for permission check.", e);
        }
    }
}
