package com.sequenceiq.freeipa.entity.json;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;

public class JsonStringSetUtils {

    private JsonStringSetUtils() {
    }

    public static Set<String> jsonToStringSet(Json json) {
        if (json != null) {
            try {
                return json.get(Set.class);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to parse permission String set from Json.", e);
            }
        }
        return Collections.emptySet();
    }

    public static Json stringSetToJson(Set<String> stringSet) {
        try {
            return new Json(stringSet);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to create Json from String set.", e);
        }
    }
}
