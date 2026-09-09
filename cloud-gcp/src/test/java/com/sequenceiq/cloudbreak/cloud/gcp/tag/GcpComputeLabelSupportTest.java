package com.sequenceiq.cloudbreak.cloud.gcp.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.tag.GcpComputeLabelSupport.LabelSnapshot;
import com.sequenceiq.cloudbreak.cloud.gcp.tag.GcpComputeLabelSupport.LabelWriter;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

class GcpComputeLabelSupportTest {

    private static final String RESOURCE_KIND = "disk";

    private static final String RESOURCE_NAME = "test-resource";

    private static final String FINGERPRINT = "fingerprint";

    private final Logger logger = mock(Logger.class);

    private final TagUpdateStrategy tagUpdateStrategy = tagUpdateStrategy();

    @Test
    void testUpdateLabelsMergesAndWritesWhenNotUpToDate() throws Exception {
        LabelSnapshot snapshot = new LabelSnapshot(FINGERPRINT, Map.of("existingKey", "existingValue"));
        AtomicReference<Map<String, String>> writtenLabels = new AtomicReference<>();
        AtomicReference<String> writtenFingerprint = new AtomicReference<>();

        GcpComputeLabelSupport.updateLabels(tagUpdateStrategy, logger, RESOURCE_KIND, RESOURCE_NAME, Map.of("newKey", "newValue"),
                () -> snapshot,
                (labels, fingerprint) -> {
                    writtenLabels.set(labels);
                    writtenFingerprint.set(fingerprint);
                });

        assertEquals(Map.of("existingKey", "existingValue", "newKey", "newValue"), writtenLabels.get());
        assertEquals(FINGERPRINT, writtenFingerprint.get());
    }

    @Test
    void testUpdateLabelsSkipsWriteWhenAlreadyUpToDate() throws Exception {
        LabelSnapshot snapshot = new LabelSnapshot(FINGERPRINT, Map.of("existingKey", "existingValue"));
        LabelWriter writer = mock(LabelWriter.class);

        GcpComputeLabelSupport.updateLabels(tagUpdateStrategy, logger, RESOURCE_KIND, RESOURCE_NAME, Map.of("existingKey", "existingValue"),
                () -> snapshot, writer);

        verify(writer, never()).write(any(), any());
    }

    @Test
    void testDeleteLabelsRemovesRequestedKeysAndWrites() throws Exception {
        LabelSnapshot snapshot = new LabelSnapshot(FINGERPRINT, Map.of("keyToDelete", "value", "survivorKey", "survivorValue"));
        AtomicReference<Map<String, String>> writtenLabels = new AtomicReference<>();

        GcpComputeLabelSupport.deleteLabels(tagUpdateStrategy, logger, RESOURCE_KIND, RESOURCE_NAME, Set.of("keyToDelete"),
                () -> snapshot,
                (labels, fingerprint) -> writtenLabels.set(labels));

        assertEquals(Map.of("survivorKey", "survivorValue"), writtenLabels.get());
    }

    @Test
    void testDeleteLabelsSkipsWriteWhenNoKeysPresent() throws Exception {
        LabelSnapshot snapshot = new LabelSnapshot(FINGERPRINT, Map.of("otherKey", "otherValue"));
        LabelWriter writer = mock(LabelWriter.class);

        GcpComputeLabelSupport.deleteLabels(tagUpdateStrategy, logger, RESOURCE_KIND, RESOURCE_NAME, Set.of("keyToDelete"),
                () -> snapshot, writer);

        verify(writer, never()).write(any(), any());
    }

    private static TagUpdateStrategy tagUpdateStrategy() {
        return new TagUpdateStrategy() {
            @Override
            public Set<ResourceType> supportedTypes() {
                return Set.of();
            }

            @Override
            public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags)
                    throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void deleteTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Set<String> tagKeys)
                    throws IOException {
                throw new UnsupportedOperationException();
            }
        };
    }
}
