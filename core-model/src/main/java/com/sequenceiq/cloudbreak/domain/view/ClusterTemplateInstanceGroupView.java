package com.sequenceiq.cloudbreak.domain.view;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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
