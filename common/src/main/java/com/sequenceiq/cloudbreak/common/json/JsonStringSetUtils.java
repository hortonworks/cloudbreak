package com.sequenceiq.cloudbreak.common.json;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

public class JsonStringSetUtils {

    private JsonStringSetUtils() {
    }

    public static Set<String> jsonToStringSet(Json json) {
        if (json != null) {
            try {
                return json.get(Set.class);
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to parse permission String set from Json.", e);
            }
        }
        return Collections.emptySet();
    }

    public static Json stringSetToJson(Set<String> stringSet) {
        return new Json(stringSet);
    }
}
