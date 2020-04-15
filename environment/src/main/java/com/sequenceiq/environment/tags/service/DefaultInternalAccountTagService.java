package com.sequenceiq.environment.tags.service;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.environment.api.v1.tags.model.AccountTagStatus;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponse;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponses;
import com.sequenceiq.environment.tags.domain.AccountTag;

@Service
public class DefaultInternalAccountTagService {

    private static final String ACCOUNT_TAG_PATTERN = "^(?!microsoft|azure|aws|windows|\\\\s)[^,]*$";

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
            accountTagResponse.setStatus(AccountTagStatus.DEFAULT);
            responses.add(accountTagResponse);
        }
        internalAccountTagResponses = new AccountTagResponses(responses);
    }

    private AccountTagResponses getDefaults() {
        Set<AccountTagResponse> accountTagResponses = new HashSet<>();

        if (applyInternalTags) {
            accountTagResponses = internalAccountTagResponses
                    .getResponses()
                    .stream()
                    .collect(Collectors.toSet());
        }
        return new AccountTagResponses(accountTagResponses);
    }

    public void merge(List<AccountTagResponse> accountTagResponses) {
        for (AccountTagResponse response : getDefaults().getResponses()) {
            Optional<AccountTagResponse> first = accountTagResponses
                    .stream()
                    .filter(e -> e.getKey().equals(response.getKey()))
                    .findFirst();
            if (!first.isPresent()) {
                accountTagResponses.add(response);
            }
        }
    }

    public void validate(List<AccountTag> accountTags) {
        for (AccountTagResponse defaultTag : getDefaults().getResponses()) {
            Optional<AccountTag> requestContainsUnmodifiableTag = accountTags
                    .stream()
                    .filter(e -> e.getTagKey().equals(defaultTag.getKey()))
                    .findFirst();
            if (requestContainsUnmodifiableTag.isPresent()) {
                throw new BadRequestException(String.format("Tag with %s key exist as an unmodifiable tag.", defaultTag.getKey()));
            }
        }
        for (AccountTag accountTag : accountTags) {
            Pattern pattern = Pattern.compile(ACCOUNT_TAG_PATTERN);
            Matcher keyMatcher = pattern.matcher(accountTag.getTagKey());
            Matcher valueMatcher = pattern.matcher(accountTag.getTagValue());
            if (!keyMatcher.matches()) {
                throw new BadRequestException(
                        String.format("The key '%s' can only can not start with microsoft or azure or aws or windows or space", accountTag.getTagKey()));
            }
            if (!valueMatcher.matches()) {
                throw new BadRequestException(
                        String.format("The value '%s' can only can not start with microsoft or azure or aws or windows or space", accountTag.getTagValue()));
            }
        }
    }
}
