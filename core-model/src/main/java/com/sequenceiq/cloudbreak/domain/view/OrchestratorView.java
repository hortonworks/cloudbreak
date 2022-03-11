package com.sequenceiq.cloudbreak.domain.view;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

@Entity
@Table(name = "Orchestrator")
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

    @Override
    public String toString() {
        return "OrchestratorView{" +
                "id=" + id +
                ", type='" + type + '\'' +
                '}';
    }
}
