package com.sequenceiq.freeipa.service.image;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PreferredOsService {

    @Value("${freeipa.image.catalog.default.os}")
    private String defaultOs;

    public String getDefaultOs() {
        return defaultOs;
    }

    public String getPreferredOs(String requestedOs) {
        if (StringUtils.isNotBlank(requestedOs)) {
            return requestedOs;
        } else {
            return getDefaultOs();
        }
    }
}
