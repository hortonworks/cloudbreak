package com.sequenceiq.datalake.entity;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import com.sequenceiq.datalake.converter.DatalakeStatusEnumConverter;

@Entity
@Table(name = "sdxstatus")
public class SdxStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sdx_status_generator")
    @SequenceGenerator(name = "sdx_status_generator", sequenceName = "sdxstatus_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datalake")
    private SdxCluster datalake;

    private Long created;

    private String statusReason;

    @NotNull
    @Convert(converter = DatalakeStatusEnumConverter.class)
    private DatalakeStatusEnum status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SdxCluster getDatalake() {
        return datalake;
    }

    public void setDatalake(SdxCluster datalake) {
        this.datalake = datalake;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public DatalakeStatusEnum getStatus() {
        return status;
    }

    public void setStatus(DatalakeStatusEnum status) {
        this.status = status;
    }
}
