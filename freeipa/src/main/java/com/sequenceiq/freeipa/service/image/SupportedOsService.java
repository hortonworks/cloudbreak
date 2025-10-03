package com.sequenceiq.freeipa.service.image;

import static com.sequenceiq.common.model.OsType.CENTOS7;
import static com.sequenceiq.common.model.OsType.RHEL8;
import static com.sequenceiq.common.model.OsType.RHEL9;

import org.springframework.stereotype.Service;

@Service
public class SupportedOsService {

    public boolean isSupported(String os) {
        return os == null
                || CENTOS7.getOs().equalsIgnoreCase(os)
                || CENTOS7.getOsType().equalsIgnoreCase(os)
                || RHEL8.getOs().equalsIgnoreCase(os)
                || RHEL9.getOs().equalsIgnoreCase(os);
    }

    public boolean isRhel8Supported() {
        return isSupported(RHEL8.getOs());
    }
}
