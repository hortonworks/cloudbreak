package com.sequenceiq.cloudbreak.structuredevent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import com.sequenceiq.cloudbreak.common.dal.model.AccountAwareResource;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.service.converter.CDPStructuredEventTypeConverter;

@Entity
@Table(name = "cdp_structured_event")
public class CDPStructuredEventEntity implements AccountAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "cdp_structured_event_generator")
    @SequenceGenerator(name = "cdp_structured_event_generator", sequenceName = "cdp_structured_event_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    @Convert(converter = CDPStructuredEventTypeConverter.class)
    private StructuredEventType eventType;

    @Column(nullable = false)
    private String resourceType;

    @Column(nullable = false)
    private String resourceCrn;

    @Column(nullable = false)
    private Long timestamp;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json structuredEventJson;

    @Column(nullable = false)
    private String accountId;

    public Long getId() {
        return id;
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StructuredEventType getEventType() {
        return eventType;
    }

    public void setEventType(StructuredEventType eventType) {
        this.eventType = eventType;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Json getStructuredEventJson() {
        return structuredEventJson;
    }

    public void setStructuredEventJson(Json structuredEventJson) {
        this.structuredEventJson = structuredEventJson;
    }

    @Override
    public String getName() {
        return structuredEventJson.getString("operation.resourceName");
    }

    @Override
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }
}
