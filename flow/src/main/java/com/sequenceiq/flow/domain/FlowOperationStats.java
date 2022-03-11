package com.sequenceiq.flow.domain;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.flow.api.model.operation.OperationType;
import com.sequenceiq.flow.converter.OperationTypeConverter;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"operationtype", "cloudplatform"}))
public class FlowOperationStats implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "flowoperationstats_generator")
    @SequenceGenerator(name = "flowoperationstats_generator", sequenceName = "flowoperationstats_id_seq", allocationSize = 1)
    private Long id;

    @Convert(converter = OperationTypeConverter.class)
    @Column(nullable = false)
    private OperationType operationType;

    @Column(nullable = false)
    private String cloudPlatform;

    private String durationHistory;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public String getDurationHistory() {
        return durationHistory;
    }

    public void setDurationHistory(String durationHistory) {
        this.durationHistory = durationHistory;
    }

    @Override
    public String toString() {
        return "FlowOperationStats{" +
                "id=" + id +
                ", operationType=" + operationType +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", durationHistory='" + durationHistory + '\'' +
                '}';
    }
}
