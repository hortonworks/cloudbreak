package com.sequenceiq.environment.environment.flow.modify.tags;

import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationEvent;

public final class EnvTagsModificationSupport {

    private EnvTagsModificationSupport() {
    }

    public static boolean hasTagsToRemove(Set<String> tagsToRemove) {
        return CollectionUtils.isNotEmpty(tagsToRemove);
    }

    public static EnvTagsModificationEvent nextEvent(EnvTagsModificationEvent event, String nextSelector) {
        return EnvTagsModificationEvent.builder()
                .withSelector(nextSelector)
                .withResourceId(event.getResourceId())
                .withResourceName(event.getResourceName())
                .withResourceCrn(event.getResourceCrn())
                .withUserDefinedTags(event.getUserDefinedTags())
                .withTagsToRemove(event.getTagsToRemove())
                .build();
    }
}
