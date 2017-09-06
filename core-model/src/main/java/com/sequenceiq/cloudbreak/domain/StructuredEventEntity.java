package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;

@Entity
@Table(name = "structuredevent")
public class StructuredEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "structuredevent_generator")
    @SequenceGenerator(name = "structuredevent_generator", sequenceName = "structuredevent_id_seq", allocationSize = 1)
    private Long id;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json structuredEventJson;

    public StructuredEventEntity(Json structuredEventJson) {
        this.structuredEventJson = structuredEventJson;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Json getStructuredEventJson() {
        return structuredEventJson;
    }

    public void setStructuredEventJson(Json structuredEventJson) {
        this.structuredEventJson = structuredEventJson;
    }
}
