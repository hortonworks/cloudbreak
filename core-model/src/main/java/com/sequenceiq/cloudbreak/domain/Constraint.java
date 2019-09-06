package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;

@Entity
@Table(name = "hostgroup_constraint")
public class Constraint implements ProvisionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "hostgroup_constraint_template_generator")
    @SequenceGenerator(name = "hostgroup_constraint_template_generator", sequenceName = "hostgroup_constraint_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    private InstanceGroup instanceGroup;

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

    public Integer getHostCount() {
        return instanceGroup.getNodeCount();
    }
}
