package com.sequenceiq.cloudbreak.structuredevent.service.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.structuredevent.domain.CDPStructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.repository.AccountAwareResourceRepository;
import com.sequenceiq.cloudbreak.structuredevent.repository.CDPPagingStructuredEventRepository;
import com.sequenceiq.cloudbreak.structuredevent.repository.CDPStructuredEventRepository;
import com.sequenceiq.cloudbreak.structuredevent.service.AbstractAccountAwareResourceService;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPStructuredEventService;
import com.sequenceiq.cloudbreak.structuredevent.service.converter.CDPStructuredEventEntityToCDPStructuredEventConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.converter.CDPStructuredEventToCDPStructuredEventEntityConverter;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@Component
public class CDPStructuredEventDBService extends AbstractAccountAwareResourceService<CDPStructuredEventEntity> implements CDPStructuredEventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPStructuredEventDBService.class);

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

    private ValidationResult validate(CDPStructuredEvent event) {
        ValidationResult.ValidationResultBuilder builder = ValidationResult.builder();
        if (StringUtils.isEmpty(event.getOperation().getResourceCrn())) {
            builder.error("Resource crn cannot be null or empty");
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
            Page<CDPStructuredEventEntity> events = pagingStructuredEventRepository.findByEventTypeInAndResourceCrn(types, resourceCrn, pageable);
            return (Page<T>) Optional.ofNullable(events).orElse(Page.empty()).map(event -> cdpStructuredEventEntityToCDPStructuredEventConverter
                    .convert(event));
        } catch (Exception ex) {
            String msg = String.format("Failed get pageable events for types: '%s' and resource CRN: '%s'", types, resourceCrn);
            LOGGER.warn(msg, ex);
            throw new CloudbreakServiceException(msg, ex);
        }
    }

    @Override
    public <T extends CDPStructuredEvent> List<T> getEventsOfResource(List<StructuredEventType> eventTypes, String resourceCrn) {
        LOGGER.debug("Gathering events for type: '{}' and resource CRN: '{}'", eventTypes, resourceCrn);
        List<StructuredEventType> types = getAllEventTypeIfEmpty(eventTypes);
        try {
            List<CDPStructuredEventEntity> events = structuredEventRepository.findByEventTypeInAndResourceCrn(types, resourceCrn);
            return (List<T>) Optional.ofNullable(events).orElse(new ArrayList<>()).stream()
                    .map(event -> cdpStructuredEventEntityToCDPStructuredEventConverter
                            .convert(event))
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            String msg = String.format("Failed get events for types: '%s' and resource CRN: '%s'", types, resourceCrn);
            LOGGER.warn(msg, ex);
            throw new CloudbreakServiceException(msg, ex);
        }
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
