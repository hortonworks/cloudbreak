package com.sequenceiq.freeipa.service.image;

import static com.sequenceiq.common.model.OsType.CENTOS7;
import static com.sequenceiq.common.model.OsType.RHEL8;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@Service
public class SupportedOsService {

    @Inject
    private EntitlementService entitlementService;

    @Value("${freeipa.image.catalog.default.os}")
    private String defaultOs;

    public boolean isSupported(String os) {
        //since os is not mandatory in the request, we should return true if it's not present
        return os == null || CENTOS7.getOs().equalsIgnoreCase(os) || CENTOS7.getOsType().equalsIgnoreCase(os) || RHEL8.getOs().equalsIgnoreCase(os);
    }

    public boolean isRhel8Supported() {
        return isSupported(RHEL8.getOs());
    }
}
