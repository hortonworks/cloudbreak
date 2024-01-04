package com.sequenceiq.flow.domain;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

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
}
