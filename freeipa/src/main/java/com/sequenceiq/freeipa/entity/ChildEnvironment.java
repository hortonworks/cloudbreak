package com.sequenceiq.freeipa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.common.dal.model.AccountIdAwareResource;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"environmentcrn"}))
public class ChildEnvironment implements AccountIdAwareResource {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "childenvironment_generator")
    @SequenceGenerator(name = "childenvironment_generator", sequenceName = "childenvironment_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "stack_id")
    private Stack stack;

    @Column(nullable = false)
    private String environmentCrn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    @Override
    public String getAccountId() {
        return stack.getAccountId();
    }

    @Override
    public String toString() {
        return "ChildEnvironment{" +
                "id=" + id +
                ", stack=" + stack +
                ", environmentCrn='" + environmentCrn + '\'' +
                '}';
    }
}
