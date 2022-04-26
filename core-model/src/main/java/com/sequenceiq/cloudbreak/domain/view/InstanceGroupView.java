package com.sequenceiq.cloudbreak.domain.view;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.converter.InstanceGroupTypeConverter;
import com.sequenceiq.cloudbreak.converter.ScalabilityOptionConverter;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ScalabilityOption;

@Entity
@Table(name = "InstanceGroup")
public class InstanceGroupView implements ProvisionEntity {
    @Id

    private Long id;

    @ManyToOne
    private StackApiView stack;

    @Column
    private String groupName;

    @OneToOne(fetch = FetchType.LAZY)
    private Template template;

    @OneToOne(fetch = FetchType.LAZY)
    private SecurityGroup securityGroup;

    @Convert(converter = InstanceGroupTypeConverter.class)
    private InstanceGroupType instanceGroupType = InstanceGroupType.CORE;

    @OneToMany(mappedBy = "instanceGroup", fetch = FetchType.EAGER)
    private Set<InstanceMetaDataView> instanceMetaData = new HashSet<>();

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json attributes;

    private int minimumNodeCount;

    @OneToOne(fetch = FetchType.LAZY, orphanRemoval = true)
    private InstanceGroupNetwork instanceGroupNetwork;

    //@ElementCollection(fetch = FetchType.EAGER)
    //@Column(name = "availabilityzone")
    //private Set<String> availabilityZones = new HashSet<>();

    @Convert(converter = ScalabilityOptionConverter.class)
    private ScalabilityOption scalabilityOption = ScalabilityOption.ALLOWED;

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

    public InstanceGroupType getInstanceGroupType() {
        return instanceGroupType;
    }

    public Long getId() {
        return id;
    }

    public Template getTemplate() {
        return template;
    }

    public SecurityGroup getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(SecurityGroup securityGroup) {
        this.securityGroup = securityGroup;
    }

    public void setInstanceGroupType(InstanceGroupType instanceGroupType) {
        this.instanceGroupType = instanceGroupType;
    }

    public Json getAttributes() {
        return attributes;
    }

    public int getMinimumNodeCount() {
        return minimumNodeCount;
    }

    public InstanceGroupNetwork getInstanceGroupNetwork() {
        return instanceGroupNetwork;
    }

    //public Set<String> getAvailabilityZones() {
    //    return availabilityZones;
    //}

    public ScalabilityOption getScalabilityOption() {
        return scalabilityOption;
    }
}
