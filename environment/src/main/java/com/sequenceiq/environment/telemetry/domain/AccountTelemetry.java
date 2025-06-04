package com.sequenceiq.environment.telemetry.domain;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Where;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.auth.security.AuthResource;
import com.sequenceiq.cloudbreak.common.dal.model.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.model.SensitiveLoggingComponent;

@Entity
@Where(clause = "archived = false")
@Table
public class AccountTelemetry implements Serializable, AuthResource, AccountIdAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accounttelemetry_generator")
    @SequenceGenerator(name = "accounttelemetry_generator", sequenceName = "accounttelemetry_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String resourceCrn;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json rules;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json features;

    private String enabledSensitiveStorageLogs;

    private boolean archived;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Features getFeatures() {
        if (features != null && features.getValue() != null) {
            return JsonUtil.readValueOpt(features.getValue(), Features.class).orElse(null);
        }
        return null;
    }

    public void setFeatures(Features features) {
        if (features != null) {
            this.features = new Json(features);
        }
    }

    public List<AnonymizationRule> getRules() {
        if (rules != null && rules.getValue() != null) {
            return JsonUtil.jsonToTypeOpt(rules.getValue(), new AnonymizationRulesTypeReference()).orElse(null);
        }
        return null;
    }

    public void setRules(List<AnonymizationRule> rules) {
        if (rules != null) {
            this.rules = new Json(rules);
        }
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    @Override
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public String getResourceCrn() {
        return resourceCrn;
    }

    @Override
    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public Set<SensitiveLoggingComponent> getEnabledSensitiveStorageLogs() {
        if (StringUtils.isNotEmpty(enabledSensitiveStorageLogs)) {
            return Arrays.stream(enabledSensitiveStorageLogs.split(",")).map(SensitiveLoggingComponent::valueOf).collect(Collectors.toSet());
        }
        return Set.of();
    }

    public void setEnabledSensitiveStorageLogs(Set<String> enabledSensitiveStorageLogs) {
        this.enabledSensitiveStorageLogs = Joiner.on(",").join(enabledSensitiveStorageLogs);
    }

    @Override
    public String toString() {
        return "AccountTelemetry{" +
                "id=" + id +
                ", accountId='" + accountId + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                ", rules=" + rules +
                ", features=" + features +
                ", archived=" + archived +
                ", enabledSensitiveStorageLogs=" + enabledSensitiveStorageLogs +
                '}';
    }
}
