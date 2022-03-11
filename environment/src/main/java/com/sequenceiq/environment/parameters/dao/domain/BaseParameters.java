package com.sequenceiq.environment.parameters.dao.domain;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sequenceiq.environment.environment.domain.EnvironmentView;

@Entity
@Table(name = "environment_parameters")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "parameters_platform")
public abstract class BaseParameters {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "environment_parameters_generator")
    @SequenceGenerator(name = "environment_parameters_generator", sequenceName = "environment_parameters_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToOne
    @JsonIgnore
    @JoinColumn(nullable = false)
    private EnvironmentView environment;

    @Column(nullable = false)
    private String accountId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EnvironmentView getEnvironment() {
        return environment;
    }

    public void setEnvironment(EnvironmentView environment) {
        this.environment = environment;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", environment=" + getIfNotNull(environment, EnvironmentView::getResourceCrn) +
                ", accountId='" + accountId + '\'' +
                '}';
    }
}
