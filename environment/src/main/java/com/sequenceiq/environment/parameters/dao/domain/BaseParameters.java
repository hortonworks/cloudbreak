package com.sequenceiq.environment.parameters.dao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sequenceiq.environment.environment.domain.EnvironmentView;

@Entity
@Table(name = "environment_parameters")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "parameters_platform")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes({ @JsonSubTypes.Type(value = AwsParameters.class, name = "awsParameters"),
        @JsonSubTypes.Type(value = AzureParameters.class, name = "azureParameters"),
        @JsonSubTypes.Type(value = GcpParameters.class, name = "gcpParameters"),
        @JsonSubTypes.Type(value = YarnParameters.class, name = "yarnParameters") })
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

    @Column(name = "distribution_list")
    private String distributionList;

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

    public String getDistributionList() {
        return distributionList;
    }

    public void setDistributionList(String distributionList) {
        this.distributionList = distributionList;
    }
}
