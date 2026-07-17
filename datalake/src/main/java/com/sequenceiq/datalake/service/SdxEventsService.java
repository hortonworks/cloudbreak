package com.sequenceiq.datalake.service;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.events.EventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.db.CDPStructuredEventDBService;
import com.sequenceiq.datalake.entity.SdxCluster;

@Service
public class SdxEventsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxEventsService.class);

    @Inject
    private CDPStructuredEventDBService cdpStructuredEventDBService;

    @Inject
    private EventV4Endpoint eventV4Endpoint;

    @Inject
    private SdxEventsHelper sdxEventsHelper;

    public List<CDPStructuredEvent> getPagedDatalakeAuditEvents(String environmentCrn, List<StructuredEventType> types, Integer page, Integer size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        sdxEventsHelper.ensureNonDeletedNonDetachedDatalakeExists(environmentCrn);
        List<SdxCluster> datalakes = sdxEventsHelper.getAvailableAndDetachedDatalakes(environmentCrn);

        List<CDPStructuredEvent> dlEvents = retrievePagableDatalakeServiceEvents(types,
                datalakes.stream()
                        .map(SdxCluster::getCrn).collect(toList()), pageable);
        List<List<CDPStructuredEvent>> cbEvents = datalakes.stream()
                .map(datalake -> retrievePagedCloudbreakServiceEvents(datalake, page, size))
                .toList();

        List<CDPStructuredEvent> combinedEvents = new ArrayList<>();
        cbEvents.forEach(combinedEvents::addAll);
        combinedEvents.addAll(dlEvents);

        if (combinedEvents.isEmpty()) {
            LOGGER.info("No events from datalake and cloudbreak service");
            return List.of();
        }
        return sortAndFilterBasedOnPageSize(combinedEvents, size);
    }

    private List<CDPStructuredEvent> retrievePagableDatalakeServiceEvents(List<StructuredEventType> types, List<String> datalakeCrns, PageRequest pageable) {
        Page<CDPStructuredEvent> pagedResponse = cdpStructuredEventDBService.getPagedEventsOfResources(types, datalakeCrns, pageable);
        if (pagedResponse != null && !pagedResponse.getContent().isEmpty()) {
            return pagedResponse.getContent();
        } else {
            LOGGER.info("No events from datalake service");
            return List.of();
        }
    }

    private List<CDPStructuredEvent> retrievePagedCloudbreakServiceEvents(SdxCluster sdxCluster, Integer page, Integer size) {
        if (sdxCluster.getDeleted() != null) {
            return Collections.emptyList();
        }

        try {
            List<CloudbreakEventV4Response> cloudbreakEventV4Responses = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> eventV4Endpoint.getPagedCloudbreakEventListByCrn(
                            sdxEventsHelper.getCloudbreakCrn(sdxCluster),
                            page,
                            size,
                            false
                    )
            );
            return cloudbreakEventV4Responses.stream()
                    .map(entry -> sdxEventsHelper.convert(entry, sdxCluster.getCrn())).collect(toList());
        } catch (NotFoundException | jakarta.ws.rs.NotFoundException notFoundException) {
            LOGGER.error("Failed to retrieve paged cloudbreak service events due to not found exception!", notFoundException);
            return Collections.emptyList();
        } catch (Exception exception) {
            LOGGER.error("Failed to retrieve paged cloudbreak service events!", exception);
            throw new CloudbreakServiceException("Failed to retrieve paged cloudbreak service events!", exception);
        }
    }

    private List<CDPStructuredEvent> sortAndFilterBasedOnPageSize(List<CDPStructuredEvent> eventList, Integer size) {
        return eventList.stream()
                .sorted(Collections.reverseOrder(Comparator.comparingLong(f -> f.getOperation().getTimestamp())))
                .collect(toList())
                .subList(0, (eventList.size() > size) ? size : eventList.size());
    }

}
