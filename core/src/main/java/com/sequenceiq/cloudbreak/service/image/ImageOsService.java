package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.common.model.OsType.CENTOS7;
import static com.sequenceiq.common.model.OsType.RHEL8;
import static com.sequenceiq.common.model.OsType.RHEL9;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageOsService {

    @Value("${cb.image.catalog.default.os}")
    private String defaultOs;

    public boolean isSupported(String os) {
        //since os is not mandatory in the request, we should return true if it's not present
        return os == null || CENTOS7.getOs().equalsIgnoreCase(os) || CENTOS7.getOsType().equalsIgnoreCase(os) ||
                RHEL8.getOs().equalsIgnoreCase(os) ||
                RHEL9.getOs().equalsIgnoreCase(os);
    }

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
