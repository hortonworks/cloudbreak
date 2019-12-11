package com.sequenceiq.freeipa.entity;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.service.secret.domain.AccountIdAwareResource;

@Entity
public class UserSyncStatus implements AccountIdAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "usersyncstatus_generator")
    @SequenceGenerator(name = "usersyncstatus_generator", sequenceName = "usersyncstatus_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne
    private Stack stack;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json umsEventGenerationIds;

    private Long lastFullSyncStartTime;

    private Long lastFullSyncEndTime;

    public UserSyncStatus() {
    }

    public UserSyncStatus(Stack stack) {
        this.stack = stack;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public Json getUmsEventGenerationIds() {
        return umsEventGenerationIds;
    }

    public void setUmsEventGenerationIds(Json umsEventGenerationIds) {
        this.umsEventGenerationIds = umsEventGenerationIds;
    }

    public Long getLastFullSyncStartTime() {
        return lastFullSyncStartTime;
    }

    public void setLastFullSyncStartTime(Long lastFullSyncStartTime) {
        this.lastFullSyncStartTime = lastFullSyncStartTime;
    }

    public Long getLastFullSyncEndTime() {
        return lastFullSyncEndTime;
    }

    public void setLastFullSyncEndTime(Long lastFullSyncEndTime) {
        this.lastFullSyncEndTime = lastFullSyncEndTime;
    }

    @Override
    public String getAccountId() {
        return stack.getAccountId();
    }
}
