package com.sequenceiq.cloudbreak.structuredevent.service.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;
import com.sequenceiq.cloudbreak.common.dal.repository.AccountAwareResourceRepository;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.service.account.AbstractAccountAwareResourceService;
import com.sequenceiq.cloudbreak.structuredevent.domain.CDPStructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.repository.CDPPagingStructuredEventRepository;
import com.sequenceiq.cloudbreak.structuredevent.repository.CDPStructuredEventRepository;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPStructuredEventService;
import com.sequenceiq.cloudbreak.structuredevent.service.converter.CDPStructuredEventEntityToCDPStructuredEventConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.converter.CDPStructuredEventToCDPStructuredEventEntityConverter;
import com.sequenceiq.cloudbreak.util.Benchmark;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@Component
public class CDPStructuredEventDBService extends AbstractAccountAwareResourceService<CDPStructuredEventEntity> implements CDPStructuredEventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPStructuredEventDBService.class);

    @Value("${cdp.structuredevent.maxsize:20000}")
    private int maxSize;

    @Inject
    private CDPStructuredEventRepository structuredEventRepository;

    @Inject
    private CDPPagingStructuredEventRepository pagingStructuredEventRepository;

    @Inject
    private CDPStructuredEventToCDPStructuredEventEntityConverter cdpStructuredEventToCDPStructuredEventEntityConverter;

    @Inject
    private CDPStructuredEventEntityToCDPStructuredEventConverter cdpStructuredEventEntityToCDPStructuredEventConverter;

    @Override
    public void create(CDPStructuredEvent structuredEvent) {
        if (structuredEvent != null && CDPStructuredNotificationEvent.class.getSimpleName().equals(structuredEvent.getType())) {
            LOGGER.info("Stored StructuredEvent type: {}, payload: {}", structuredEvent.getType(),
                    AnonymizerUtil.anonymize(JsonUtil.writeValueAsStringSilent(structuredEvent)));
            ValidationResult validationResult = validate(structuredEvent);
            if (validationResult.hasError()) {
                LOGGER.warn(validationResult.getFormattedErrors());
            } else {
                CDPStructuredEventEntity structuredEventEntityEntity = cdpStructuredEventToCDPStructuredEventEntityConverter
                        .convert(structuredEvent);
                create(structuredEventEntityEntity, structuredEventEntityEntity.getAccountId());
            }
        }
    }

    private ValidationResult validate(CDPStructuredEvent event) {
        ValidationResult.ValidationResultBuilder builder = ValidationResult.builder();

        if (!StringUtils.hasText(event.getOperation().getResourceCrn()) ||
                !Crn.isCrn(event.getOperation().getResourceCrn())) {
            builder.error("Resource crn cannot be null or empty or invalid");
        }
        return builder.build();
    }

    public boolean isEnabled() {
        return true;
    }

    @Override
    public AccountAwareResourceRepository<CDPStructuredEventEntity, Long> repository() {
        return structuredEventRepository;
    }

    @Override
    protected void prepareDeletion(CDPStructuredEventEntity resource) {

    }

    @Override
    protected void prepareCreation(CDPStructuredEventEntity resource) {

    }

    @Override
    public <T extends CDPStructuredEvent> Page<T> getPagedEventsOfResource(List<StructuredEventType> eventTypes, String resourceCrn, Pageable pageable) {
        LOGGER.debug("Gathering pageable events for types: '{}' and resource CRN: '{}'", eventTypes, resourceCrn);
        List<StructuredEventType> types = getAllEventTypeIfEmpty(eventTypes);
        try {
            Slice<CDPStructuredEventEntity> events = Benchmark.measureAndWarnIfLong(() ->
                pagingStructuredEventRepository.findByEventTypeInAndResourceCrn(types, resourceCrn, pageable),
                LOGGER,
                String.format("getPagedEventsOfResource for resourceCrn='%s', types=%s, pageable=%s", resourceCrn, types, pageable)
            );
            List<T> content = (List<T>) Optional.ofNullable(events)
                    .map(Slice::stream)
                    .orElse(Stream.empty())
                    .map(event -> cdpStructuredEventEntityToCDPStructuredEventConverter.convert(event))
                    .collect(Collectors.toList());
            return new PageImpl<>(content, pageable, content.size());
        } catch (Exception ex) {
            String msg = String.format("Failed get pageable events for types: '%s' and resource CRN: '%s'", types, resourceCrn);
            LOGGER.warn(msg, ex);
            throw new CloudbreakServiceException(msg, ex);
        }
    }

    @Override
    public <T extends CDPStructuredEvent> Page<T> getPagedEventsOfResources(List<StructuredEventType> eventTypes, List<String> resourceCrns, Pageable pageable) {
        LOGGER.debug("Gathering pageable events for types: '{}' and resource CRNs: '{}'", eventTypes, resourceCrns);
        List<StructuredEventType> types = getAllEventTypeIfEmpty(eventTypes);
        try {
            Slice<CDPStructuredEventEntity> events = Benchmark.measureAndWarnIfLong(() ->
                pagingStructuredEventRepository.findByEventTypeInAndResourceCrnIn(types, resourceCrns, pageable),
                LOGGER,
                String.format("getPagedEventsOfResources for resourceCrns='%s', types=%s, pageable=%s", resourceCrns, types, pageable)
            );
            List<T> content = (List<T>) Optional.ofNullable(events)
                    .map(Slice::stream)
                    .orElse(Stream.empty())
                    .map(cdpStructuredEventEntityToCDPStructuredEventConverter::convert)
                    .collect(Collectors.toList());
            return new PageImpl<>(content, pageable, content.size());
        } catch (Exception ex) {
            String msg = String.format("Failed get pageable events for types: '%s' and resource CRNs: '%s'", types, resourceCrns);
            LOGGER.warn(msg, ex);
            throw new CloudbreakServiceException(msg, ex);
        }
    }

    @Override
    public <T extends CDPStructuredEvent> List<T> getEventsOfResource(List<StructuredEventType> eventTypes, String resourceCrn) {
        LOGGER.debug("Gathering events for type: '{}' and resource CRN: '{}'", eventTypes, resourceCrn);
        List<StructuredEventType> types = getAllEventTypeIfEmpty(eventTypes);
        try {
            Slice<CDPStructuredEventEntity> events = Benchmark.measureAndWarnIfLong(() ->
                pagingStructuredEventRepository.findByEventTypeInAndResourceCrn(types, resourceCrn, createPageRequest()),
                LOGGER,
                String.format("getEventsOfResource for resourceCrn='%s', types=%s", resourceCrn, types)
            );
            return (List<T>) Optional.ofNullable(events)
                    .map(Slice::stream)
                    .orElse(Stream.empty())
                    .map(cdpStructuredEventEntityToCDPStructuredEventConverter::convert)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            String msg = String.format("Failed get events for types: '%s' and resource CRN: '%s'", types, resourceCrn);
            LOGGER.warn(msg, ex);
            throw new CloudbreakServiceException(msg, ex);
        }
    }

    @Override
    public <T extends CDPStructuredEvent> List<T> getEventsOfResources(List<StructuredEventType> eventTypes, List<String> resourceCrns) {
        LOGGER.debug("Gathering events for type: '{}' and resource CRN's: '{}'", eventTypes, resourceCrns);
        List<StructuredEventType> types = getAllEventTypeIfEmpty(eventTypes);
        try {
            Slice<CDPStructuredEventEntity> events = Benchmark.measureAndWarnIfLong(() ->
                pagingStructuredEventRepository.findByEventTypeInAndResourceCrnIn(types, resourceCrns, createPageRequest()),
                LOGGER,
                String.format("getEventsOfResources for resourceCrns='{}', types={}", resourceCrns, types)
            );
            return (List<T>) Optional.ofNullable(events)
                    .map(Slice::stream)
                    .orElse(Stream.empty())
                    .map(cdpStructuredEventEntityToCDPStructuredEventConverter::convert)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            String msg = String.format("Failed get events for types: '%s' and resource CRNs: '%s'", types, resourceCrns);
            LOGGER.warn(msg, ex);
            throw new CloudbreakServiceException(msg, ex);
        }
    }

    public Optional<Exception> deleteStructuredEventByResourceCrnThatIsOlderThan(String resourceCrn, long since) {
        Optional<Exception> exception = Optional.empty();
        try {
            LOGGER.trace("About to delete structured event(s) for CRN: {} that has a smaller - or equal - timestamp than the following: {}", resourceCrn, since);
            structuredEventRepository.deleteByResourceCrnOlderThan(resourceCrn, since);
        } catch (Exception e) {
            LOGGER.debug("deleteByResourceCrnOlderThan() repository deletion has failed for {} due to: {}",
                    CDPStructuredEventEntity.class.getSimpleName(), e.getMessage());
            exception = Optional.of(e);
        }
        return exception;
    }

    public void deleteStructuredEventByResourceCrn(String resourceCrn) {
        try {
            LOGGER.debug("About to delete structured event(s) for resource CRN: {}", resourceCrn);
            structuredEventRepository.deleteByResourceCrn(resourceCrn);
        } catch (Exception e) {
            LOGGER.debug("deleteByResourceCrnOlderThan() repository deletion has failed for {} due to: {}",
                    CDPStructuredEventEntity.class.getSimpleName(), e.getMessage());
            throw e;
        }
    }

    private PageRequest createPageRequest() {
        return PageRequest.of(0, maxSize, Sort.by("timestamp").descending());
    }

    private List<StructuredEventType> getAllEventTypeIfEmpty(List<StructuredEventType> eventTypes) {
        List<StructuredEventType> types = new ArrayList<>(eventTypes);
        if (CollectionUtils.isEmpty(eventTypes)) {
            LOGGER.info("We need to add all structured event types to the filter");
            types.add(StructuredEventType.NOTIFICATION);
            types.add(StructuredEventType.REST);
            types.add(StructuredEventType.FLOW);
        }
        return types;
    }

}
