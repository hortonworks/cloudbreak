package com.sequenceiq.periscope.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;

import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.converter.AdjustmentTypeConverter;

@Entity
@NamedQueries({
        @NamedQuery(name = "ScalingPolicy.findByCluster", query = "SELECT c FROM ScalingPolicy c WHERE c.alert.cluster.id= :clusterId AND c.id= :policyId"),
        @NamedQuery(name = "ScalingPolicy.findAllByCluster", query = "SELECT c FROM ScalingPolicy c WHERE c.alert.cluster.id= :id")
})
public class ScalingPolicy implements Clustered {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "policy_generator")
    @SequenceGenerator(name = "policy_generator", sequenceName = "scalingpolicy_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    @Convert(converter = AdjustmentTypeConverter.class)
    @Column(name = "adjustment_type")
    private AdjustmentType adjustmentType;

    @Column(name = "scaling_adjustment")
    private int scalingAdjustment;

    @OneToOne(mappedBy = "scalingPolicy")
    private BaseAlert alert;

    @Column(name = "host_group")
    private String hostGroup;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public AdjustmentType getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(AdjustmentType adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    public int getScalingAdjustment() {
        return scalingAdjustment;
    }

    public void setScalingAdjustment(int scalingAdjustment) {
        this.scalingAdjustment = scalingAdjustment;
    }

    public BaseAlert getAlert() {
        return alert;
    }

    public void setAlert(BaseAlert alert) {
        this.alert = alert;
    }

    public String getHostGroup() {
        return hostGroup;
    }

    public void setHostGroup(String hostGroup) {
        this.hostGroup = hostGroup;
    }

    public long getAlertId() {
        return alert.getId();
    }

    @Override
    public Cluster getCluster() {
        return alert.getCluster();
    }
}
