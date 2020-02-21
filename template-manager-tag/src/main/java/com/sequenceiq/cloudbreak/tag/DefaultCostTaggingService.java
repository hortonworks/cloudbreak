package com.sequenceiq.cloudbreak.tag;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.tag.request.CDPTagGenerationRequest;
import com.sequenceiq.cloudbreak.tag.request.CDPTagMergeRequest;

@Service
public class DefaultCostTaggingService implements CostTagging {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCostTaggingService.class);

    @Inject
    private CentralTagUpdater centralTagUpdater;

    @Override
    public Map<String, String> prepareDefaultTags(CDPTagGenerationRequest request) {
        LOGGER.debug("About to prepare default tag(s)...");
        Map<String, String> result = new HashMap<>();
        String platform = request.getPlatform();
        validateResourceTagsNotContainTheSameTag(request.getUserDefinedTags(), request.getAccountTags());
        addCDPCrnIfPresent(result, DefaultApplicationTag.ENVIRONMENT_CRN, request.getEnvironmentCrn(), platform);
        addCDPCrnIfPresent(result, DefaultApplicationTag.CREATOR_CRN, request.getCreatorCrn(), platform);
        addCDPCrnIfPresent(result, DefaultApplicationTag.RESOURCE_CRN, request.getResourceCrn(), platform);

        TagPreparationObject preparationObject = TagPreparationObject.Builder.builder()
                .withAccountId(request.getAccountId())
                .withCloudPlatform(request.getPlatform())
                .withResourceCrn(request.getResourceCrn())
                .withUserName(request.getUserName())
                .withUserCrn(request.getCreatorCrn())
                .build();

        for (Map.Entry<String, String> entry : request.getAccountTags().entrySet()) {
            LOGGER.debug("The tag template is: {}", entry.getValue());
            String generatedValue = centralTagUpdater.getTagText(preparationObject, entry.getValue());
            LOGGER.debug("The generated tag value is: {}", generatedValue);
            addAccountTag(result, entry.getKey(), generatedValue, request.getPlatform());
        }
        LOGGER.debug("The following default tag(s) has prepared: {}", result);
        return result;
    }

    @Override
    public Map<String, String> mergeTags(CDPTagMergeRequest request) {
        LOGGER.debug("About to merge tag(s)...");
        Map<String, String> result = new HashMap<>();
        result.putAll(request.getRequestTags());
        for (String key : request.getEnvironmentTags().keySet()) {
            addTagIfNotPresented(result, request, key);
        }
        LOGGER.debug("The following requested tag(s) will be applied prepared: {}", result);
        return result;
    }

    private void addTagIfNotPresented(Map<String, String> result, CDPTagMergeRequest request, String key) {
        if (request.isKeyNotPresented(key)) {
            LOGGER.debug("Adding {} tag to default tags.", key);
            result.put(transform(key, request.getPlatform()), transform(request.getEnvironmentTags().get(key), request.getPlatform()));
        }
    }

    private void addCDPCrnIfPresent(Map<String, String> result, DefaultApplicationTag tag, String crn, String platform) {
        if (StringUtils.isNotEmpty(crn)) {
            LOGGER.debug("Adding  crn {} tag to default tags.", crn);
            result.put(transform(tag.key(), platform), crn);
        } else {
            LOGGER.debug("Unable to add \"{}\" - cost - tag to the resource's default tags because it's value is empty or null!", tag.key());
        }
    }

    private void addAccountTag(Map<String, String> result, String key, String value, String platform) {
        LOGGER.debug("Adding account tag with key {} and value {}.", key, value);
        result.put(transform(key, platform), transform(value, platform));
    }

    private String transform(String value, String platform) {
        String valueAfterCheck = Strings.isNullOrEmpty(value) ? "unknown" : value;
        return "GCP".equalsIgnoreCase(platform)
                ? valueAfterCheck.split("@")[0].toLowerCase().replaceAll("[^\\w]", "-") : valueAfterCheck;
    }

    private void validateResourceTagsNotContainTheSameTag(Map<String, String> userDefinedResourceTags, Map<String, String> accountTags) {
        LOGGER.debug("Validating that there is no resource tag defined with the same key what is defined as account tag.");
        if (!CollectionUtils.isEmpty(userDefinedResourceTags) && !CollectionUtils.isEmpty(accountTags)) {
            Set<String> accountTagsDuplicatedInResourceTags = accountTags
                    .keySet()
                    .stream()
                    .filter(userDefinedResourceTags::containsKey)
                    .collect(Collectors.toSet());
            if (!accountTagsDuplicatedInResourceTags.isEmpty()) {
                String msg = String.format("The request must not contain tag(s) with key: '%s', because with the same key tag has already been defined "
                        + "on account level!", String.join(", ", accountTagsDuplicatedInResourceTags));
                LOGGER.info(msg);
                throw new AccountTagValidationFailed(msg);
            }
        }
    }
}
