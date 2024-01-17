package com.sequenceiq.cloudbreak.common.request;

import java.util.Optional;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class HeaderValueProvider {
    private HeaderValueProvider() {
    }

    public static String getHeaderOrItsFallbackValueOrDefault(String headerName, String fallback, String defaultValue) {
        return getHeaderValueFromRequestContext(headerName)
                .orElse(getHeaderValueFromRequestContext(fallback)
                        .orElse(defaultValue));

    }

    public static Optional<String> getHeaderValueFromRequestContext(String headerName) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return Optional.ofNullable(attributes.getRequest().getHeader(headerName));
        }
        return Optional.empty();
    }

}
