package com.sequenceiq.freeipa.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

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
