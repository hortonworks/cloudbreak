package com.sequenceiq.cloudbreak.template.views;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.common.api.type.InstanceGroupType;

public class HostgroupView {

    private final String name;

    private final Integer volumeCount;

    private final boolean instanceGroupConfigured;

    private final InstanceGroupType instanceGroupType;

    private final Integer nodeCount;

    private final SortedSet<String> hosts;

    private final Set<VolumeTemplate> volumeTemplates;

    private final TemporaryStorage temporaryStorage;

    private final Integer temporaryStorageVolumeCount;

    public HostgroupView(String name, int volumeCount, InstanceGroupType instanceGroupType, Collection<String> hosts) {
        this(name, volumeCount, instanceGroupType, hosts, Collections.emptySet());
    }

    public HostgroupView(String name, int volumeCount, InstanceGroupType instanceGroupType,
            Collection<String> hosts, Set<VolumeTemplate> volumeTemplates) {
        this(name, volumeCount, instanceGroupType, hosts, volumeTemplates, null, null);
    }

    public HostgroupView(String name, int volumeCount, InstanceGroupType instanceGroupType,
            Collection<String> hosts, Set<VolumeTemplate> volumeTemplates, TemporaryStorage temporaryStorage, Integer temporaryStorageVolumeCount) {
        this.name = name;
        this.volumeCount = volumeCount;
        instanceGroupConfigured = true;
        this.instanceGroupType = instanceGroupType;
        this.hosts = hosts != null
                ? Collections.unmodifiableSortedSet(new TreeSet<>(filterNullOrEmpty(hosts)) {
            @Override
            public String toString() {
                return String.join(",", this);
            }
        })
                : Collections.emptySortedSet();
        nodeCount = this.hosts.size();
        this.volumeTemplates = Collections.unmodifiableSet(volumeTemplates);
        this.temporaryStorage = temporaryStorage;
        this.temporaryStorageVolumeCount = temporaryStorageVolumeCount;
    }

    public HostgroupView(String name, int volumeCount, InstanceGroupType instanceGroupType, Integer nodeCount) {
        this.name = name;
        this.volumeCount = volumeCount;
        instanceGroupConfigured = true;
        this.instanceGroupType = instanceGroupType;
        this.nodeCount = nodeCount;
        hosts = Collections.emptySortedSet();
        volumeTemplates = Collections.emptySet();
        temporaryStorage = null;
        temporaryStorageVolumeCount = null;
    }

    public HostgroupView(String name) {
        this.name = name;
        volumeCount = null;
        instanceGroupConfigured = false;
        instanceGroupType = InstanceGroupType.CORE;
        nodeCount = 0;
        hosts = Collections.emptySortedSet();
        volumeTemplates = Collections.emptySet();
        temporaryStorage = null;
        temporaryStorageVolumeCount = null;
    }

    public String getName() {
        return name;
    }

    public Integer getVolumeCount() {
        return volumeCount;
    }

    public boolean isInstanceGroupConfigured() {
        return instanceGroupConfigured;
    }

    public InstanceGroupType getInstanceGroupType() {
        return instanceGroupType;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public SortedSet<String> getHosts() {
        return hosts;
    }

    public Set<VolumeTemplate> getVolumeTemplates() {
        return volumeTemplates;
    }

    public TemporaryStorage getTemporaryStorage() {
        return temporaryStorage;
    }

    public Integer getTemporaryStorageVolumeCount() {
        return temporaryStorageVolumeCount;
    }

    private List<String> filterNullOrEmpty(Collection<String> hosts) {
        return hosts.stream().filter(StringUtils::isNotEmpty).collect(Collectors.toList());
    }
}
