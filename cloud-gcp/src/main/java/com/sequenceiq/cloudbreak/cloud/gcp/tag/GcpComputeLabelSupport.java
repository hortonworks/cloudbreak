package com.sequenceiq.cloudbreak.cloud.gcp.tag;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;

/**
 * Shared read-merge-write / read-remove-write orchestration for GCP compute label update/delete strategies.
 */
final class GcpComputeLabelSupport {

    private GcpComputeLabelSupport() {
    }

    static void updateLabels(TagUpdateStrategy tagUpdateStrategy, Logger logger, String resourceKind, String resourceName,
            Map<String, String> newLabels, LabelSnapshotFetcher fetcher, LabelWriter writer) throws IOException {
        LabelSnapshot snapshot = fetcher.fetch();
        Map<String, String> existingLabels = existingLabels(snapshot);
        if (tagUpdateStrategy.tagsAlreadyUpToDate(existingLabels, newLabels)) {
            logger.debug("Labels for {} {} are already up to date, skipping update.", resourceKind, resourceName);
        } else {
            Map<String, String> mergedLabels = tagUpdateStrategy.mergeTags(existingLabels, newLabels);
            writer.write(mergedLabels, snapshot.fingerprint());
        }
    }

    static void deleteLabels(TagUpdateStrategy tagUpdateStrategy, Logger logger, String resourceKind, String resourceName,
            Set<String> labelKeys, LabelSnapshotFetcher fetcher, LabelWriter writer) throws IOException {
        LabelSnapshot snapshot = fetcher.fetch();
        Map<String, String> existingLabels = existingLabels(snapshot);
        if (tagUpdateStrategy.hasTagKeysToDelete(existingLabels, labelKeys)) {
            Map<String, String> remainingLabels = tagUpdateStrategy.removeTagKeys(existingLabels, labelKeys);
            tagUpdateStrategy.logTagDeletion(logger, resourceName, labelKeys, existingLabels, remainingLabels.keySet());
            writer.write(remainingLabels, snapshot.fingerprint());
        } else {
            logger.debug("No labels to delete for {} {}, skipping.", resourceKind, resourceName);
        }
    }

    private static Map<String, String> existingLabels(LabelSnapshot snapshot) {
        return snapshot.labels() != null ? snapshot.labels() : Map.of();
    }

    record LabelSnapshot(String fingerprint, Map<String, String> labels) {
    }

    @FunctionalInterface
    interface LabelSnapshotFetcher {
        LabelSnapshot fetch() throws IOException;
    }

    @FunctionalInterface
    interface LabelWriter {
        void write(Map<String, String> labels, String fingerprint) throws IOException;
    }
}
