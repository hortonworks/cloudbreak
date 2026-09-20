package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.tag;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;

import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;

/**
 * Shared read-check-apply / read-check-remove orchestration for AWS tagged-resource tag update and delete strategies
 * (EFS, KMS, Secrets Manager, ELB) that expose list/tag/untag style tag APIs. The per-resource client creation, tag
 * fetch and tag/untag calls stay in the strategies (as suppliers/runnables); only the duplicated up-to-date check and
 * delete logging live here.
 */
final class AwsTaggedResourceTagSupport {

    private AwsTaggedResourceTagSupport() {
    }

    static void updateTags(TagUpdateStrategy tagUpdateStrategy, Logger logger, String resourceLabel, String resourceIdentifier,
            Map<String, String> tags, Supplier<Map<String, String>> existingTagsSupplier, Runnable tagApplier) {
        Map<String, String> existingTags = existingTags(existingTagsSupplier);
        if (tagUpdateStrategy.tagsAlreadyUpToDate(existingTags, tags)) {
            logger.info("Tags for {} {} are already up to date, skipping update.", resourceLabel, resourceIdentifier);
        } else {
            tagApplier.run();
        }
    }

    static void deleteTags(TagUpdateStrategy tagUpdateStrategy, Logger logger, String resourceLabel, String resourceIdentifier,
            Set<String> tagKeys, Supplier<Map<String, String>> existingTagsSupplier, Runnable tagRemover) {
        Map<String, String> existingTags = existingTags(existingTagsSupplier);
        if (tagUpdateStrategy.hasTagKeysToDelete(existingTags, tagKeys)) {
            Map<String, String> remainingTags = tagUpdateStrategy.removeTagKeys(existingTags, tagKeys);
            tagUpdateStrategy.logTagDeletion(logger, resourceLabel + " " + resourceIdentifier, tagKeys, existingTags, remainingTags.keySet());
            tagRemover.run();
        } else {
            logger.info("No tags to delete for {} {}, skipping.", resourceLabel, resourceIdentifier);
        }
    }

    private static Map<String, String> existingTags(Supplier<Map<String, String>> existingTagsSupplier) {
        Map<String, String> existingTags = existingTagsSupplier.get();
        return existingTags != null ? existingTags : Map.of();
    }
}
