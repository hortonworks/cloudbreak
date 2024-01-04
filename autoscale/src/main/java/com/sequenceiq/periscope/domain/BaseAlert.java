package com.sequenceiq.periscope.domain;

import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;

import com.sequenceiq.periscope.api.model.AlertType;

@Entity
@DiscriminatorColumn(name = "alert_type")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class BaseAlert implements Clustered {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "alert_generator")
    @SequenceGenerator(name = "alert_generator", sequenceName = "alert_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    private Cluster cluster;

    private String name;

    private String description;

    @Column(name = "alert_crn")
    private String alertCrn;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private ScalingPolicy scalingPolicy;

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

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ScalingPolicy getScalingPolicy() {
        return scalingPolicy;
    }

    public void setScalingPolicy(ScalingPolicy scalingPolicy) {
        this.scalingPolicy = scalingPolicy;
    }

    public Long getScalingPolicyId() {
        return scalingPolicy == null ? null : scalingPolicy.getId();
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public abstract AlertType getAlertType();

    public String getAlertCrn() {
        return alertCrn;
    }

    public void setAlertCrn(String alertCrn) {
        this.alertCrn = alertCrn;
    }

    public abstract Map<String, String> getTelemetryParameters();
}
