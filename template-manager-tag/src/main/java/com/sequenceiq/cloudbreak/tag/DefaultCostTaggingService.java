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

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.tag.request.CDPTagGenerationRequest;
import com.sequenceiq.cloudbreak.tag.request.CDPTagMergeRequest;
import com.sequenceiq.common.api.tag.model.Tags;

@Service
public class DefaultCostTaggingService implements CostTagging {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCostTaggingService.class);

    @Inject
    private CentralTagUpdater centralTagUpdater;

    @Override
    public Tags prepareDefaultTags(CDPTagGenerationRequest request) {
        LOGGER.debug("About to prepare default tag(s)...");
        Tags result = new Tags();
        String platform = request.getPlatform();
        validateResourceTagsNotContainTheSameTag(request.getUserDefinedTags(), request.getAccountTags());
        addCDPCrnIfPresent(result, DefaultApplicationTag.ENVIRONMENT_CRN, request.getEnvironmentCrn(), platform);
        addCDPCrnIfPresent(result, DefaultApplicationTag.CREATOR_CRN, request.getCreatorCrn(), platform);
        addCDPCrnIfPresent(result, DefaultApplicationTag.RESOURCE_CRN, request.getResourceCrn(), platform);

        Tags accountTagResult = generateAccountTags(request);
        for (Map.Entry<String, String> entry : accountTagResult.getAll().entrySet()) {
            addAccountTag(result, entry.getKey(), entry.getValue(), request.getPlatform());
        }
        LOGGER.debug("The following default tag(s) has prepared: {}", result);
        return result;
    }

    @Override
    public Tags generateAccountTags(CDPTagGenerationRequest request) {
        Map<String, String> result = new HashMap<>();

        TagPreparationObject preparationObject = TagPreparationObject.Builder.builder()
                .withAccountId(request.getAccountId())
                .withCloudPlatform(request.getPlatform())
                .withResourceCrn(request.getResourceCrn())
                .withUserName(request.getUserName())
                .withUserCrn(request.getCreatorCrn())
                .build();

        for (Map.Entry<String, String> entry : Tags.getAll(request.getAccountTags()).entrySet()) {
            LOGGER.debug("The tag template is: {}", entry.getValue());
            String generatedValue = centralTagUpdater.getTagText(preparationObject, entry.getValue());
            LOGGER.debug("The generated tag value is: {}", generatedValue);
            result.put(entry.getKey(), generatedValue);
        }
        return new Tags(result);
    }

    @Override
    public Tags mergeTags(CDPTagMergeRequest request) {
        LOGGER.debug("About to merge tag(s)...");
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : Tags.getAll(request.getRequestTags()).entrySet()) {
            if (!keyOrValueIsEmpty(entry.getKey(), entry.getValue())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        for (Map.Entry<String, String> entry : Tags.getAll(request.getEnvironmentTags()).entrySet()) {
            if (!keyOrValueIsEmpty(entry.getKey(), entry.getValue())) {
                addTagIfNotPresented(result, request, entry.getKey());
            }
        }
        LOGGER.debug("The following requested tag(s) will be applied prepared: {}", result);
        return new Tags(result);
    }

    private boolean keyOrValueIsEmpty(String key, String value) {
        return (key == null || key.isBlank()) || (value == null || value.isBlank());
    }

    private void addTagIfNotPresented(Map<String, String> result, CDPTagMergeRequest request, String key) {
        if (request.isKeyNotPresented(key)) {
            LOGGER.debug("Adding {} tag to default tags.", key);
            result.put(transform(key, request.getPlatform()), transform(request.getEnvironmentTags().getTagValue(key), request.getPlatform()));
        }
    }

    private void addCDPCrnIfPresent(Tags result, DefaultApplicationTag tag, String crn, String platform) {
        if (StringUtils.isNotEmpty(crn)) {
            LOGGER.debug("Adding  crn {} tag to default tags.", crn);
            result.addTag(transform(tag.key(), platform), crn);
        } else {
            LOGGER.debug("Unable to add \"{}\" - cost - tag to the resource's default tags because it's value is empty or null!", tag.key());
        }
    }

    private void addAccountTag(Tags result, String key, String value, String platform) {
        LOGGER.debug("Adding account tag with key {} and value {}.", key, value);
        if (!keyOrValueIsEmpty(key, value)) {
            result.addTag(transform(key, platform), transform(value, platform));
        }
    }

    private String transform(String value, String platform) {
        String valueAfterCheck = Strings.isNullOrEmpty(value) ? "unknown" : value;
        return "GCP".equalsIgnoreCase(platform)
                ? valueAfterCheck.split("@")[0].toLowerCase().replaceAll("[^\\w]", "-") : valueAfterCheck;
    }

    private void validateResourceTagsNotContainTheSameTag(Tags userDefinedResourceTags, Tags accountTags) {
        LOGGER.debug("Validating that there is no resource tag defined with the same key what is defined as account tag.");
        if (userDefinedResourceTags != null && !userDefinedResourceTags.isEmpty() && accountTags != null && !accountTags.isEmpty()) {
            Set<String> accountTagsDuplicatedInResourceTags = accountTags
                    .getKeys()
                    .stream()
                    .filter(userDefinedResourceTags::hasTag)
                    .collect(Collectors.toSet());
            if (!accountTagsDuplicatedInResourceTags.isEmpty()) {

                Set<String> duplicateTagsWithDiffValues = accountTagsDuplicatedInResourceTags
                    .stream()
                    .filter(s -> !accountTags.getTagValue(s).equals(userDefinedResourceTags.getTagValue(s)))
                    .collect(Collectors.toSet());

                if (!duplicateTagsWithDiffValues.isEmpty()) {
                    String msg = String.format(
                        "The request must not contain tag(s) with key: '%s', because with the same key tag has already been defined "
                            + "on account level!",
                        String.join(", ", duplicateTagsWithDiffValues));
                    LOGGER.info(msg);
                    throw new AccountTagValidationFailed(msg);
                }
            }
        }
    }
}
