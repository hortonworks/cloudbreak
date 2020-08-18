package com.sequenceiq.cloudbreak.structuredevent.service.db;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.structuredevent.domain.CDPStructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.repository.CDPPagingStructuredEventRepository;
import com.sequenceiq.cloudbreak.structuredevent.repository.CDPStructuredEventRepository;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEventContainer;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredRestCallEvent;
import com.sequenceiq.cloudbreak.structuredevent.repository.AccountAwareResourceRepository;
import com.sequenceiq.cloudbreak.structuredevent.service.AbstractAccountAwareResourceService;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPStructuredEventService;

@Component
public class CDPStructuredEventDBService extends AbstractAccountAwareResourceService<CDPStructuredEventEntity> implements CDPStructuredEventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPStructuredEventDBService.class);

    @Inject
    private ConversionService conversionService;

    @Inject
    private CDPStructuredEventRepository structuredEventRepository;

    @Inject
    private CDPPagingStructuredEventRepository pagingStructuredEventRepository;


    @Override
    public void create(CDPStructuredEvent structuredEvent) {
        LOGGER.info("Stored StructuredEvent type: {}, payload: {}", structuredEvent.getType(),
                AnonymizerUtil.anonymize(JsonUtil.writeValueAsStringSilent(structuredEvent)));
        CDPStructuredEventEntity structuredEventEntityEntity = conversionService.convert(structuredEvent, CDPStructuredEventEntity.class);
        create(structuredEventEntityEntity, structuredEventEntityEntity.getAccountId());
    }

    public boolean isEnabled() {
        return true;
    }

    @Override
    public <T extends CDPStructuredEvent> List<T> getEventsForAccountWithType(String accountId, Class<T> eventClass) {
        List<CDPStructuredEventEntity> events = structuredEventRepository.findByAccountIdAndEventType(accountId, StructuredEventType.getByCDPClass(eventClass));
        return events != null ? (List<T>) conversionService.convert(events,
                TypeDescriptor.forObject(events),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(CDPStructuredEvent.class))) : Collections.emptyList();
    }

    @Override
    public <T extends CDPStructuredEvent> List<T> getEventsForAccountWithTypeSince(String accountId, Class<T> eventClass, Long since) {
        List<CDPStructuredEventEntity> events = structuredEventRepository.findByAccountIdAndEventTypeSince(accountId,
                StructuredEventType.getByCDPClass(eventClass), since);
        return events != null ? (List<T>) conversionService.convert(events,
                TypeDescriptor.forObject(events),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(CDPStructuredEvent.class))) : Collections.emptyList();
    }

    @Override
    public <T extends CDPStructuredEvent> List<T> getEventsWithTypeAndResourceId(Class<T> eventClass, String resourceType, Long resourceId) {
        List<CDPStructuredEventEntity> events = structuredEventRepository
                .findByEventTypeAndResourceTypeAndResourceId(StructuredEventType.getByCDPClass(eventClass), resourceType, resourceId);
        return events != null ? (List<T>) conversionService.convert(events,
                TypeDescriptor.forObject(events),
                TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(CDPStructuredEvent.class))) : Collections.emptyList();
    }

    @Override
    public <T extends CDPStructuredEvent> Page<T> getEventsLimitedWithTypeAndResourceId(Class<T> eventClass, String resourceType, Long resourceId, Pageable pageable) {
        Page<CDPStructuredEventEntity> events = pagingStructuredEventRepository
                .findByEventTypeAndResourceTypeAndResourceId(StructuredEventType.getByCDPClass(eventClass), resourceType, resourceId, pageable);
        return (Page<T>) Optional.ofNullable(events).orElse(Page.empty()).map(event -> conversionService.convert(event, CDPStructuredEvent.class));
    }

    @Override
    public CDPStructuredEventContainer getEventsForUserWithResourceId(String resourceType, Long resourceId) {
        List<CDPStructuredRestCallEvent> rest = getEventsWithTypeAndResourceId(CDPStructuredRestCallEvent.class, resourceType, resourceId);
        List<CDPStructuredFlowEvent> flow = getEventsWithTypeAndResourceId(CDPStructuredFlowEvent.class, resourceType, resourceId);
        List<CDPStructuredNotificationEvent> notification
                = getEventsWithTypeAndResourceId(CDPStructuredNotificationEvent.class, resourceType, resourceId);
        return new CDPStructuredEventContainer(flow, rest, notification);
    }

    @Override
    public CDPStructuredEventContainer getStructuredEventsForObject(String name, String accountId) {
        return null;
    }

    @Override
    public Page<CDPStructuredNotificationEvent> getPagedNotificationEventsOfResource(StructuredEventType eventType, String resourceCrn,
            Pageable pageable) {
//        pagingStructuredEventRepository
        return null;
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
}
