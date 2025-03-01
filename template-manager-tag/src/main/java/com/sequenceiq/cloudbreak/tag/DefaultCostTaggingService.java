package com.sequenceiq.cloudbreak.tag;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.tag.request.CDPTagGenerationRequest;
import com.sequenceiq.cloudbreak.tag.request.CDPTagMergeRequest;
import com.sequenceiq.common.model.DefaultApplicationTag;

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
        addCDPTagIfPresent(result, DefaultApplicationTag.ENVIRONMENT_CRN, request.getEnvironmentCrn(), platform);
        addCDPTagIfPresent(result, DefaultApplicationTag.CREATOR_CRN, request.getCreatorCrn(), platform);
        addCDPTagIfPresent(result, DefaultApplicationTag.RESOURCE_CRN, request.getResourceCrn(), platform);
        addCDPTagIfPresent(result, DefaultApplicationTag.RESOURCE_ID, request.getResourceId(), platform);

        Map<String, String> accountTagResult = generateAccountTags(request);
        for (Map.Entry<String, String> entry : accountTagResult.entrySet()) {
            addAccountTag(result, entry.getKey(), entry.getValue(), request.getPlatform());
        }
        LOGGER.debug("The following default tag(s) has prepared: {}", result);
        return result;
    }

    @Override
    public Map<String, String> generateAccountTags(CDPTagGenerationRequest request) {
        Map<String, String> result = new HashMap<>();

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
            addTagToResult(result, entry.getKey(), generatedValue, request.getPlatform());
        }
        return result;
    }

    @Override
    public Map<String, String> mergeTags(CDPTagMergeRequest request) {
        LOGGER.debug("About to merge tag(s)...");
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : request.getRequestTags().entrySet()) {
            if (!keyOrValueIsEmpty(entry.getKey(), entry.getValue())) {
                addTagToResult(result, entry.getKey(), entry.getValue(), request.getPlatform());
            }
        }
        for (Map.Entry<String, String> entry : request.getEnvironmentTags().entrySet()) {
            if (!keyOrValueIsEmpty(entry.getKey(), entry.getValue())) {
                addTagIfNotPresented(result, request, entry.getKey());
            }
        }
        LOGGER.debug("The following requested tag(s) will be applied prepared: {}", result);
        return result;
    }

    private boolean keyOrValueIsEmpty(String key, String value) {
        return key == null || key.isBlank() || value == null || value.isBlank();
    }

    private void addTagIfNotPresented(Map<String, String> result, CDPTagMergeRequest request, String key) {
        if (request.isKeyNotPresented(key)) {
            LOGGER.debug("Adding {} tag to default tags.", key);
            addTagToResult(result, key, request.getEnvironmentTags().get(key), request.getPlatform());
        }
    }

    private void addCDPTagIfPresent(Map<String, String> result, DefaultApplicationTag tag, String value, String platform) {
        if (StringUtils.isNotEmpty(value)) {
            LOGGER.debug("Adding {} {} tag to default tags.", tag.key(), value);
            addTagToResult(result, tag.key(), value, platform);
        } else {
            LOGGER.debug("Unable to add \"{}\" - cost - tag to the resource's default tags because it's value is empty or null!", tag.key());
        }
    }

    private void addAccountTag(Map<String, String> result, String key, String value, String platform) {
        LOGGER.debug("Adding account tag with key {} and value {}.", key, value);
        if (!keyOrValueIsEmpty(key, value)) {
            addTagToResult(result, key, value, platform);
        }
    }

    private void addTagToResult(Map<String, String> result, String key, String value, String platform) {
        result.put(transform(key, platform), transform(value, platform));
    }

    private String transform(String value, String platform) {
        return Strings.isNullOrEmpty(value) ? "unknown" : value;
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

                Set<String> duplicateTagsWithDiffValues = accountTagsDuplicatedInResourceTags
                    .stream()
                    .filter(s -> !accountTags.get(s).equals(userDefinedResourceTags.get(s)))
                    .collect(Collectors.toSet());

                if (!duplicateTagsWithDiffValues.isEmpty()) {
                    String msg = String.format(
                        "The request contains tag(s) with key: '%s', because with the same key tag has already been defined "
                            + "on account level so overriding that one!",
                        String.join(", ", duplicateTagsWithDiffValues));
                    LOGGER.info(msg);
                }
            }
        }
    }
}
