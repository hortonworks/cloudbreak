package com.sequenceiq.cloudbreak.rotation.entity;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;

@Entity
public class SecretRotationStepProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "secretrotationstepprogress_generator")
    @SequenceGenerator(name = "secretrotationstepprogress_generator", sequenceName = "secretrotationstepprogress_id_seq", allocationSize = 1)
    private Long id;

    private String resourceCrn;

    @Convert(converter = RotationEnumConverter.class)
    private SecretType secretType;

    @Convert(converter = RotationEnumConverter.class)
    private SecretRotationStep secretRotationStep;

    @Convert(converter = RotationFlowExecutionTypeConverter.class)
    private RotationFlowExecutionType currentExecutionType;

    @Convert(converter = SecretRotationStepProgressStatusConverter.class)
    private SecretRotationStepProgressStatus status;

    public SecretRotationStepProgress() {
    }

    public SecretRotationStepProgress(String resourceCrn, SecretType secretType, SecretRotationStep secretRotationStep,
            RotationFlowExecutionType currentExecutionType, SecretRotationStepProgressStatus status) {
        this.resourceCrn = resourceCrn;
        this.secretType = secretType;
        this.secretRotationStep = secretRotationStep;
        this.currentExecutionType = currentExecutionType;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public SecretType getSecretType() {
        return secretType;
    }

    public void setSecretType(SecretType secretType) {
        this.secretType = secretType;
    }

    public SecretRotationStep getSecretRotationStep() {
        return secretRotationStep;
    }

    public void setSecretRotationStep(SecretRotationStep secretRotationStep) {
        this.secretRotationStep = secretRotationStep;
    }

    public RotationFlowExecutionType getCurrentExecutionType() {
        return currentExecutionType;
    }

    public void setCurrentExecutionType(RotationFlowExecutionType currentExecutionType) {
        this.currentExecutionType = currentExecutionType;
    }

    public SecretRotationStepProgressStatus getStatus() {
        return status;
    }

    public void setStatus(SecretRotationStepProgressStatus status) {
        this.status = status;
    }
}
