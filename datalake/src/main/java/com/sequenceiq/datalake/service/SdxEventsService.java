package com.sequenceiq.datalake.service;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.EventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPOperationDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.db.CDPStructuredEventDBService;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.SdxService;

@Service
public class SdxEventsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxEventsService.class);

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxService sdxService;

    @Inject
    private CDPStructuredEventDBService cdpStructuredEventDBService;

    @Inject
    private EventV4Endpoint eventV4Endpoint;

    public List<CDPStructuredEvent> getDatalakeAuditEvents(String environmentCrn, List<StructuredEventType> types, Integer page, Integer size) {
        List<CDPStructuredEvent> dlEvents;
        List<CDPStructuredEvent> cbEvents;

        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        SdxCluster sdxCluster = getDatalake(environmentCrn);
        if (sdxCluster == null) {
            LOGGER.error("Datalake not found for environment with Crn:{}", environmentCrn);
            return List.of();
        }

        List<String> datalakeCrns = getDatalakeCrns(environmentCrn);
        dlEvents = retrieveDatalakeServiceEvents(types, datalakeCrns, pageable);
        cbEvents = retrieveCloudbreakServiceEvents(sdxCluster, page, size);

        List<CDPStructuredEvent> combinedEvents = new ArrayList<>();
        combinedEvents.addAll(cbEvents);
        combinedEvents.addAll(dlEvents);

        if (combinedEvents.isEmpty()) {
            LOGGER.info("No events from datalake and cloudbreak service");
            return List.of();
        }
        return sortAndFilterBasedOnPageSize(combinedEvents, size);
    }

    private List<CDPStructuredEvent> retrieveDatalakeServiceEvents(List<StructuredEventType> types, List<String> datalakeCrns, PageRequest pageable) {
        Page<CDPStructuredEvent> pagedResponse = cdpStructuredEventDBService.getPagedEventsOfResources(types, datalakeCrns, pageable);
        if (pagedResponse != null && pagedResponse.getContent() != null && pagedResponse.getContent().size() > 0) {
            return pagedResponse.getContent();
        } else {
            LOGGER.info("No events from datalake service");
            return List.of();
        }
    }

    private List<CDPStructuredEvent> retrieveCloudbreakServiceEvents(SdxCluster sdxCluster, Integer page, Integer size) {
        List<CloudbreakEventV4Response> cloudbreakEventV4Responses = null;
        try {
            cloudbreakEventV4Responses = eventV4Endpoint.getPagedCloudbreakEventListByStack(sdxCluster.getName(), page, size,
                    getAccountId(sdxCluster.getEnvCrn()));
        } catch (Exception exception) {
            cloudbreakEventV4Responses = List.of();
        }
        // Translate the cloudbreak events
        return cloudbreakEventV4Responses.stream().map(entry -> convert(entry, sdxCluster.getCrn())).collect(Collectors.toList());
    }

    private List<CDPStructuredEvent> sortAndFilterBasedOnPageSize(List<CDPStructuredEvent> eventList, Integer size) {
        return eventList.stream().sorted(Comparator.comparingLong(f -> f.getOperation().getTimestamp())).collect(Collectors.toList())
                .subList(0, (eventList.size() > size) ? size : eventList.size());
    }

    private String getAccountId(String crnString) {
        try {
            Crn crn = Crn.safeFromString(crnString);
            return crn.getAccountId();
        } catch (NullPointerException | CrnParseException e) {
            throw new BadRequestException("Can not parse CRN to find account ID: " + crnString);
        }
    }

    /**
     * Get datalake CRNs that are or were provisioned within the environment identified by the provided CRN.
     *
     * @param environmentCrn an environment CRN
     * @return a list of Data Lake CRNs related to the environment
     */
    private List<String> getDatalakeCrns(String environmentCrn) {
        return sdxClusterRepository.findByAccountIdAndEnvCrn(getAccountId(environmentCrn), environmentCrn).stream()
                .map(SdxCluster::getCrn)
                .collect(toList());
    }

    /**
     * Get SdxCluster that is provisioned within the environment that is non-deleted and non-detached.
     *
     * @param environmentCrn an environment CRN
     * @return SdxCluster related to the environment.
     */
    private SdxCluster getDatalake(String environmentCrn) {
        LOGGER.info("Looking for datalake associated with environment Crn {}", environmentCrn);
        List<SdxCluster> sdxClusters = sdxService.listSdxByEnvCrn(environmentCrn);
        sdxClusters.stream().forEach(sdxCluster -> LOGGER.info("Found SDX cluster {}", sdxCluster));
        if (!sdxClusters.isEmpty()) {
            return sdxClusters.get(0);
        }
        return null;
    }

    /**
     * Converts a collection of {@code CloudbreakEventV4Response} to {@code CDPStructuredEvent}.
     *
     * @param cloudbreakEventV4Response Event response from cloudbreak.
     * @param datalakeCrn               Crn of data lake.
     * @return CDP structured Event
     */
    private CDPStructuredEvent convert(CloudbreakEventV4Response cloudbreakEventV4Response, String datalakeCrn) {
        CDPStructuredNotificationEvent cdpStructuredNotificationEvent = new CDPStructuredNotificationEvent();
        CDPOperationDetails cdpOperationDetails = new CDPOperationDetails();
        cdpOperationDetails.setTimestamp(cloudbreakEventV4Response.getEventTimestamp());
        cdpOperationDetails.setEventType(StructuredEventType.NOTIFICATION);
        cdpOperationDetails.setResourceName(cloudbreakEventV4Response.getClusterName());
        cdpOperationDetails.setResourceId(cloudbreakEventV4Response.getClusterId());
        cdpOperationDetails.setResourceCrn(datalakeCrn);
        cdpOperationDetails.setResourceType(CloudbreakEventService.DATALAKE_RESOURCE_TYPE);

        cdpStructuredNotificationEvent.setOperation(cdpOperationDetails);
        cdpStructuredNotificationEvent.setStatusReason(cloudbreakEventV4Response.getEventMessage());

        return cdpStructuredNotificationEvent;
    }
}
