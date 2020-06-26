package com.sequenceiq.datalake.entity.operation;

import java.util.UUID;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import com.sequenceiq.datalake.converter.SdxOperationTypeEnumConverter;
import com.sequenceiq.datalake.converter.SdxOperationStatusTypeEnumConverter;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"operationId"}))
public class SdxOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sdx_status_generator")
    @SequenceGenerator(name = "sdx_status_generator", sequenceName = "sdxstatus_id_seq", allocationSize = 1)
    private Long id;

    @NotNull
    @Convert(converter = SdxOperationTypeEnumConverter.class)
    private SdxOperationType operationType;

    @NotNull
    private Long sdxClusterId;

    private String operationId;

    private String statusReason;

    @NotNull
    @Convert(converter = SdxOperationStatusTypeEnumConverter.class)
    private SdxOperationStatus status;

    public SdxOperation() {
    }

    public SdxOperation(SdxOperationType operationType, long sdxClusterId) {
        this.operationId = UUID.randomUUID().toString();
        this.operationType = operationType;
        this.sdxClusterId = sdxClusterId;
        this.status = SdxOperationStatus.INIT;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public Long getSdxClusterId() {
        return sdxClusterId;
    }

    public SdxOperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(SdxOperationType operationType) {
        this.operationType = operationType;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public SdxOperationStatus getStatus() {
        return status;
    }

    public void setSdxClusterId(Long sdxClusterId) {
        this.sdxClusterId = sdxClusterId;
    }

    public void setStatus(SdxOperationStatus status) {
        this.status = status;
    }

}
