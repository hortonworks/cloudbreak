package com.sequenceiq.cloudbreak.domain.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

@Entity
@Table(name = "Orchestrator")
@Deprecated
public class OrchestratorView implements ProvisionEntity {
    @Id
    private Long id;

    @Column(nullable = false)
    private String type;

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

}
