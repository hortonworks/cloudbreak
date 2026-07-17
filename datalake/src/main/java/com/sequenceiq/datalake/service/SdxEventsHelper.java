package com.sequenceiq.datalake.service;

import java.util.List;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.SdxService;

@Component
public class SdxEventsHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxEventsHelper.class);

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxService sdxService;

    public void ensureNonDeletedNonDetachedDatalakeExists(String environmentCrn) {
        LOGGER.info("Looking for data lake associated with environment Crn {}", environmentCrn);
        List<SdxCluster> sdxClusters = sdxService.listSdxByEnvCrn(environmentCrn);
        sdxClusters.forEach(sdxCluster -> LOGGER.info("Found SDX cluster {}", sdxCluster));
        if (sdxClusters.isEmpty()) {
            LOGGER.info("Data Lake not found for environment with Crn:{}", environmentCrn);
            throw new NotFoundException(
                    "No non-deleted and non-detached data lake found for environment with Crn:" + environmentCrn
            );
        }
    }

    public List<SdxCluster> getAvailableAndDetachedDatalakes(String environmentCrn) {
        return sdxClusterRepository.findByAccountIdAndEnvCrn(
                getAccountId(environmentCrn),
                environmentCrn
        );
    }

    public String getCloudbreakCrn(SdxCluster sdxCluster) {
        if (StringUtils.isNotEmpty(sdxCluster.getStackCrn()) && !StringUtils.equals(sdxCluster.getStackCrn(), sdxCluster.getCrn())) {
            return sdxCluster.getStackCrn();
        }
        return sdxCluster.getCrn();
    }

    public CDPStructuredEvent convert(CloudbreakEventV4Response cloudbreakEventV4Response, String datalakeCrn) {
        CDPStructuredNotificationEvent cdpStructuredNotificationEvent = new CDPStructuredNotificationEvent();
        CDPOperationDetails cdpOperationDetails = new CDPOperationDetails();
        cdpOperationDetails.setTimestamp(cloudbreakEventV4Response.getEventTimestamp());
        cdpOperationDetails.setEventType(StructuredEventType.NOTIFICATION);
        cdpOperationDetails.setResourceName(cloudbreakEventV4Response.getClusterName());
        cdpOperationDetails.setResourceId(cloudbreakEventV4Response.getClusterId());
        cdpOperationDetails.setResourceCrn(datalakeCrn);
        cdpOperationDetails.setResourceEvent(cloudbreakEventV4Response.getEventType());
        cdpOperationDetails.setResourceType(CloudbreakEventService.DATALAKE_RESOURCE_TYPE);

        cdpStructuredNotificationEvent.setOperation(cdpOperationDetails);
        cdpStructuredNotificationEvent.setStatusReason(cloudbreakEventV4Response.getEventMessage());

        return cdpStructuredNotificationEvent;
    }

    private String getAccountId(String crnString) {
        try {
            Crn crn = Crn.safeFromString(crnString);
            return crn.getAccountId();
        } catch (NullPointerException | CrnParseException e) {
            throw new BadRequestException("Cannot parse CRN to find account ID: " + crnString);
        }
    }
}
