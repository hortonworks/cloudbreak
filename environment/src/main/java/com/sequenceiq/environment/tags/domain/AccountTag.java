package com.sequenceiq.environment.tags.domain;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Where;

import com.sequenceiq.cloudbreak.auth.security.AuthResource;
import com.sequenceiq.cloudbreak.service.secret.domain.AccountIdAwareResource;

@Entity
@Where(clause = "archived = false")
@Table
public class AccountTag implements Serializable, AuthResource, AccountIdAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accounttag_generator")
    @SequenceGenerator(name = "accounttag_generator", sequenceName = "accounttag_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String tagKey;

    @Column(nullable = false)
    private String tagValue;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String resourceCrn;

    private boolean archived;

    @Override
    public String getAccountId() {
        return accountId;
    }

    @Override
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTagKey() {
        return tagKey;
    }

    public void setTagKey(String tagKey) {
        this.tagKey = tagKey;
    }

    public String getTagValue() {
        return tagValue;
    }

    public void setTagValue(String tagValue) {
        this.tagValue = tagValue;
    }

    @Override
    public String getResourceCrn() {
        return resourceCrn;
    }

    @Override
    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AccountTag that = (AccountTag) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
