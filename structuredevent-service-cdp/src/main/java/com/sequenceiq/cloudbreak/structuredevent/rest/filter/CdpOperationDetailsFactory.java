package com.sequenceiq.cloudbreak.structuredevent.rest.filter;

import static com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType.REST;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_CRN;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_EVENT;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_ID;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_NAME;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_TYPE;

import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.flow.ha.NodeConfig;

@Component
class CdpOperationDetailsFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CdpOperationDetailsFactory.class);

    @Value("${info.app.version:}")
    private String cbVersion;

    @Inject
    private NodeConfig nodeConfig;

    public CDPOperationDetails createCDPOperationDetails(Map<String, String> restParams, Long requestTime) {
        String resourceType = null;
        String resourceId = null;
        String resourceName = null;
        String resourceCrn = null;
        String resourceEvent = null;
        if (restParams != null) {
            resourceType = restParams.get(RESOURCE_TYPE);
            resourceId = restParams.get(RESOURCE_ID);
            resourceName = restParams.get(RESOURCE_NAME);
            resourceCrn = restParams.get(RESOURCE_CRN);
            resourceEvent = restParams.get(RESOURCE_EVENT);
        }
        return new CDPOperationDetails(requestTime,
                REST,
                resourceType,
                StringUtils.isNotEmpty(resourceId) ? Long.valueOf(resourceId) : null,
                resourceName,
                nodeConfig.getId(),
                cbVersion,
                ThreadBasedUserCrnProvider.getAccountId(),
                resourceCrn,
                ThreadBasedUserCrnProvider.getUserCrn(),
                null,
                resourceEvent);

    }
}
