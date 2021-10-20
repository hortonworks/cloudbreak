package com.sequenceiq.environment.experience;

import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;
import static org.apache.commons.collections4.MapUtils.isEmpty;

import java.util.Map;

import javax.ws.rs.client.WebTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class QueryParamInjectorUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryParamInjectorUtil.class);

    private QueryParamInjectorUtil() {
    }

    public static WebTarget setQueryParams(WebTarget webTarget, Map<String, String> nameValuePairs) {
        throwIfNull(webTarget, () -> new IllegalArgumentException("Input WebTarget should not be null!"));
        if (isEmpty(nameValuePairs)) {
            LOGGER.debug("The given name - value pair map is empty hence no query param has been added to the provided WebTarget.");
            return webTarget;
        }
        for (Map.Entry<String, String> entry : nameValuePairs.entrySet()) {
            String value = entry.getValue();
            if (value != null) {
                webTarget = webTarget.queryParam(entry.getKey(), value);
            }
        }
        return webTarget;
    }

}
