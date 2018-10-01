package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;

@Entity
@Table(name = "account_preferences")
public class AccountPreferences {
    private static final String INSTANCE_TYPE_SEPARATOR = ",";

    @Id
    private String account;

    @Column(columnDefinition = "TEXT")
    private String platforms;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json defaultTags;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPlatforms() {
        return platforms;
    }

    public void setPlatforms(String platforms) {
        this.platforms = platforms;
    }

    public Json getDefaultTags() {
        return defaultTags;
    }

    public void setDefaultTags(Json defaultTags) {
        this.defaultTags = defaultTags;
    }
}
