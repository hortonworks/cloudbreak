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

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.tag.HandleBarModelKey;
import com.sequenceiq.environment.api.v1.tags.model.AccountTagStatus;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponse;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponses;
import com.sequenceiq.environment.tags.domain.AccountTag;

@Service
public class DefaultInternalAccountTagService {

    @Value("${environment.account.tag.validator.key}")
    private String keyAccountTagPattern;

    @Value("${environment.account.tag.validator.value}")
    private String valueAccountValueTagPattern;

    @Value("${environment.apply.internal.tags:true}")
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
            Pattern keyPattern = Pattern.compile(keyAccountTagPattern);
            Matcher keyMatcher = keyPattern.matcher(accountTag.getTagKey());
            Pattern valuePattern = Pattern.compile(valueAccountValueTagPattern);
            Matcher valueMatcher = valuePattern.matcher(accountTag.getTagValue());
            if (!keyMatcher.matches()) {
                throw new BadRequestException(
                        String.format("The key '%s' must start with a lowecase letter and can not start with microsoft or azure or windows "
                                + "or space and can contains only '-', '_', upper/lowercase alphanumeric characters and variables in the format of "
                                + "'{{{variable}}}'.", accountTag.getTagValue()));
            }
            if (!valueMatcher.matches()) {
                throw new BadRequestException(
                        String.format("The value '%s' can not start with space and can contains only '-' and '_', upper/lowercase alphanumeric characters "
                                + "and variables in the format of '{{{variable}}}'.", accountTag.getTagValue()));
            }
            if (isAccountTagContainsTemplate(accountTag.getTagKey()) && isAccountTagInvalid(accountTag.getTagKey())) {
                throw new BadRequestException(
                        String.format("The key '%s' of the tag contains invalid templates", accountTag.getTagKey()));
            }
            if (isAccountTagContainsTemplate(accountTag.getTagValue()) && isAccountTagInvalid(accountTag.getTagValue())) {
                throw new BadRequestException(
                        String.format("The value '%s' of the tag contains invalid templates", accountTag.getTagValue()));
            }
        }
    }

    private boolean isAccountTagContainsTemplate(String template) {
        return template.contains("{") || template.contains("}");
    }

    private boolean isAccountTagInvalid(String template) {
        template = template.replaceAll(" ", "");
        for (String modelTemplateKey : HandleBarModelKey.modelTemplateKeys()) {
            template = template.replace(modelTemplateKey, "");
        }
        return isAccountTagContainsTemplate(template);
    }
}
