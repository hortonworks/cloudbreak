package com.sequenceiq.environment.tags.service;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponse;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponses;

@Service
public class DefaultInternalAccountTagService {

    @Value("${env.apply.internal.tags:true}")
    private boolean applyInternalTags;

    private AccountTagResponses internalAccountTagResponses;

    private final CloudbreakResourceReaderService cloudbreakResourceReaderService;

    public DefaultInternalAccountTagService(CloudbreakResourceReaderService cloudbreakResourceReaderService) {
        this.cloudbreakResourceReaderService = cloudbreakResourceReaderService;
    }

    @PostConstruct
    public void init() throws IOException {
        String internalTags = cloudbreakResourceReaderService.resourceDefinition("default-internal-tags");
        Map<String, String> definition = JsonUtil.readValue(internalTags, Map.class);
        Set<AccountTagResponse> responses = new HashSet<>();
        for (Map.Entry<String, String> entry : definition.entrySet()) {
            AccountTagResponse accountTagResponse = new AccountTagResponse();
            accountTagResponse.setKey(entry.getKey());
            accountTagResponse.setValue(entry.getValue());
            responses.add(accountTagResponse);
        }
        internalAccountTagResponses = new AccountTagResponses(responses);
    }

    public AccountTagResponses getDefaults() {
        Set<AccountTagResponse> accountTagResponses = new HashSet<>();

        if (applyInternalTags) {
            accountTagResponses = internalAccountTagResponses
                    .getResponses()
                    .stream()
                    .collect(Collectors.toSet());
        }
        return new AccountTagResponses(accountTagResponses);
    }
}
