package com.sequenceiq.cloudbreak.rotation.entity;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

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

    @Convert(converter = SecretTypeConverter.class)
    private SecretType secretType;

    @Convert(converter = SecretRotationStepConverter.class)
    private SecretRotationStep secretRotationStep;

    @Convert(converter = RotationFlowExecutionTypeConverter.class)
    private RotationFlowExecutionType executionType;

    private Long created;

    private Long finished;

    public SecretRotationStepProgress() {
    }

    public SecretRotationStepProgress(String resourceCrn, SecretType secretType, SecretRotationStep secretRotationStep,
            RotationFlowExecutionType executionType, Long created) {
        this.resourceCrn = resourceCrn;
        this.secretType = secretType;
        this.secretRotationStep = secretRotationStep;
        this.executionType = executionType;
        this.created = created;
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

    public RotationFlowExecutionType getExecutionType() {
        return executionType;
    }

    public void setExecutionType(RotationFlowExecutionType executionType) {
        this.executionType = executionType;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public Long getFinished() {
        return finished;
    }

    public void setFinished(Long finished) {
        this.finished = finished;
    }
}
