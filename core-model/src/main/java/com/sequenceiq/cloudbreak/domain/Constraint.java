package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "hostgroup_constraint")
@NamedQueries({})
public class Constraint {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "constraint_template_generator")
    @SequenceGenerator(name = "constraint_template_generator", sequenceName = "hostgroup_constraint_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    private InstanceGroup instanceGroup;

    @ManyToOne
    private ConstraintTemplate constraintTemplate;

    private Integer hostCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InstanceGroup getInstanceGroup() {
        return instanceGroup;
    }

    public void setInstanceGroup(InstanceGroup instanceGroup) {
        this.instanceGroup = instanceGroup;
    }

    public ConstraintTemplate getConstraintTemplate() {
        return constraintTemplate;
    }

    public void setConstraintTemplate(ConstraintTemplate constraintTemplate) {
        this.constraintTemplate = constraintTemplate;
    }

    public Integer getHostCount() {
        return hostCount;
    }

    public void setHostCount(Integer hostCount) {
        this.hostCount = hostCount;
    }
}
