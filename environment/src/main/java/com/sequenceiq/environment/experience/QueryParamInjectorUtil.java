package com.sequenceiq.environment.experience;

import java.util.Map;

import javax.ws.rs.client.WebTarget;

public final class QueryParamInjectorUtil {

    private QueryParamInjectorUtil() {
    }

    public static WebTarget setQueryParams(WebTarget webTarget, Map<String, String> nameValuePairs) {
        WebTarget target = webTarget;
        for (Map.Entry<String, String> entry : nameValuePairs.entrySet()) {
            String value = entry.getValue();
            if (value != null) {
                target = target.queryParam(entry.getKey(), value);
            }
        }
        return target;
    }

}
