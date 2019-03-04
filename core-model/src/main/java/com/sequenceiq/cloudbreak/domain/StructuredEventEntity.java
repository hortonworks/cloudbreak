package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.WorkspaceAwareResource;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;

@Entity
@Table(name = "structuredevent")
public class StructuredEventEntity implements WorkspaceAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "structuredevent_generator")
    @SequenceGenerator(name = "structuredevent_generator", sequenceName = "structuredevent_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StructuredEventType eventType;

    @Column(nullable = false)
    private String resourceType;

    @Column(nullable = false)
    private Long resourceId;

    @Column(nullable = false)
    private Long timestamp;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json structuredEventJson;

    @ManyToOne
    private Workspace workspace;

    @ManyToOne
    @JoinColumn(name = "users_user_id")
    private User user;

    public Long getId() {
        return id;
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

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
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
    public Workspace getWorkspace() {
        return workspace;
    }

    @Override
    public String getName() {
        return resourceType;
    }

    @Override
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public WorkspaceResource getResource() {
        return WorkspaceResource.STRUCTURED_EVENT;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
