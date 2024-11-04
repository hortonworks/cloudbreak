package com.sequenceiq.distrox.v1.distrox.converter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.SecurityV4Request;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.distrox.api.v1.distrox.model.security.SecurityV1Request;

@Component
public class SecurityV1RequestToSecurityV4RequestConverter {

    public SecurityV4Request convert(SecurityV1Request request) {
        SecurityV4Request securityV4Request = new SecurityV4Request();
        if (request == null || StringUtils.isBlank(request.getSeLinux())) {
            securityV4Request.setSeLinux(SeLinux.PERMISSIVE.name());
        } else {
            securityV4Request.setSeLinux(request.getSeLinux());
        }
        return securityV4Request;
    }
}
