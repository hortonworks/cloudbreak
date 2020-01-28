package com.sequenceiq.freeipa.entity;

import com.sequenceiq.cloudbreak.service.secret.domain.AccountIdAwareResource;

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

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"childEnvironmentCrn"}))
public class ChildEnvironment implements AccountIdAwareResource {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "child_environment_generator")
    @SequenceGenerator(name = "child_environment_generator", sequenceName = "child_environment_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "parentEnvironmentCrn", referencedColumnName = "environmentCrn")
    private Stack stack;

    @Column(nullable = false)
    private String childEnvironmentCrn;

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

    public String getChildEnvironmentCrn() {
        return childEnvironmentCrn;
    }

    public void setChildEnvironmentCrn(String childEnvironmentCrn) {
        this.childEnvironmentCrn = childEnvironmentCrn;
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
                ", childEnvironmentCrn='" + childEnvironmentCrn + '\'' +
                '}';
    }
}
