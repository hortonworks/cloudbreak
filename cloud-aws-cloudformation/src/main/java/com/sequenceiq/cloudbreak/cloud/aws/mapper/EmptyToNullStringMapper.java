package com.sequenceiq.cloudbreak.cloud.aws.mapper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * AWS SDK is buggy, they send empty string in describe result,
 * but we have to set empty string to null, otherwise the request would fail
 */
@Component
public class EmptyToNullStringMapper {
    public String map(String s) {
        if (StringUtils.isBlank(s)) {
            return null;
        }
        return s;
    }
}
