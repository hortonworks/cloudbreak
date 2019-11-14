package com.sequenceiq.cloudbreak.domain.view;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

@Entity
@Table(name = "InstanceGroup")
public class InstanceGroupView implements ProvisionEntity {

    @Id
    private Long id;

    @ManyToOne
    private StackApiView stack;

    @Column
    private String groupName;

    @OneToMany(mappedBy = "instanceGroup", fetch = FetchType.EAGER)
    private Set<InstanceMetaDataView> instanceMetaData = new HashSet<>();

    public Integer getNodeCount() {
        return getNotTerminatedInstanceMetaDataSet().size();
    }

    public Set<InstanceMetaDataView> getNotTerminatedInstanceMetaDataSet() {
        return instanceMetaData.stream()
                .filter(metaData -> !metaData.isTerminated())
                .collect(Collectors.toSet());
    }

    public String getGroupName() {
        return groupName;
    }

}
