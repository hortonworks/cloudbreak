package com.sequenceiq.cloudbreak.domain.view;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

@Entity
@Table(name = "InstanceGroup")
@Deprecated
public class ClusterTemplateInstanceGroupView implements ProvisionEntity {

    @Id
    private Long id;

    @ManyToOne
    private ClusterTemplateStackApiView stack;

    @Column
    private String groupName;

    @Column
    private int initialNodeCount;

    public int getInitialNodeCount() {
        return initialNodeCount;
    }

    public String getGroupName() {
        return groupName;
    }

}
