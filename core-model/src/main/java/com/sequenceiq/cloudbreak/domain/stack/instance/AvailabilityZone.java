package com.sequenceiq.cloudbreak.domain.stack.instance;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

@Entity
@Table(name = "instancegroup_availabilityzones")
public class AvailabilityZone implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "instancegroup_availabilityzones_generator")
    @SequenceGenerator(name = "instancegroup_availabilityzones_generator", sequenceName = "instancegroup_availabilityzones_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    private InstanceGroup instanceGroup;

    @Column(name = "availabilityzone")
    private String availabilityZone;

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

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    @Override
    public String toString() {
        return "AvailabilityZone{" +
                "instanceGroupName=" + instanceGroup.getGroupName() +
                ", availabilityZone='" + availabilityZone + '\'' +
                '}';
    }
}
