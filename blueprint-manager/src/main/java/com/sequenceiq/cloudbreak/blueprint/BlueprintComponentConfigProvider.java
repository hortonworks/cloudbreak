package com.sequenceiq.cloudbreak.blueprint;

import java.io.IOException;
import java.util.Set;

import com.google.common.collect.Sets;

public interface BlueprintComponentConfigProvider {

    String configure(BlueprintPreparationObject source, String blueprintText) throws IOException;

    default boolean additionalCriteria(BlueprintPreparationObject source, String blueprintText) {
        return false;
    }

    default Set<String> components() {
        return Sets.newHashSet();
    }
}
