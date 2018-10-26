package com.sequenceiq.periscope.domain;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

@Entity
@DiscriminatorColumn(name = "alert_type")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class BaseAlert implements Clustered {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "alert_generator")
    @SequenceGenerator(name = "alert_generator", sequenceName = "alert_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    private String description;

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
        return description;
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

    @Override
    public abstract Cluster getCluster();

}
