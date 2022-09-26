package com.sequenceiq.cloudbreak.structuredevent.service.audit.extractor;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;

@Component
public class RestAuditEventSourceExtractor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestAuditEventSourceExtractor.class);

    private final Crn.Service service;

    public RestAuditEventSourceExtractor(@Value("${spring.application.name:}") String applicationName) {
        this.service = getServiceFromApplicationName(applicationName);
    }

    public Crn.Service eventSource() {
        return service;
    }

    private Crn.Service getServiceFromApplicationName(String applicationName) {
        Crn.Service result = null;
        if (StringUtils.isNotEmpty(applicationName)) {
            String serviceName = applicationName.replace("Service", "").toLowerCase();
            result = Crn.Service.fromString(serviceName);
            LOGGER.info("Determined service: '{}' as event source", result);
        }
        if (result == null) {
            String msg = String.format("The application name('${spring.application.name}'='%s') couldn't be collated as a service in CRN!", applicationName);
            LOGGER.warn(msg);
            throw new IllegalStateException(msg);
        }
        return result;
    }
}
