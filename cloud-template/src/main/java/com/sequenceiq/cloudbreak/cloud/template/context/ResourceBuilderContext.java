package com.sequenceiq.cloudbreak.cloud.template.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;
import com.sequenceiq.common.api.type.LoadBalancerType;

public class ResourceBuilderContext extends DynamicModel {

    private final Location location;

    private final String name;

    private final int parallelResourceRequest;

    private final Queue<CloudResource> networkResources = new ConcurrentLinkedQueue<>();

    private final Map<String, List<CloudResource>> groupResources = new HashMap<>();

    private final Map<Long, List<CloudResource>> computeResources = new HashMap<>();

    private final Map<LoadBalancerType, List<CloudResource>> loadBalancerResources = new HashMap<>();

    private boolean build;

    public ResourceBuilderContext(String name, Location location, int parallelResourceRequest, boolean build) {
        this(name, location, parallelResourceRequest);
        this.build = build;
    }

    public ResourceBuilderContext(String name, Location location, int parallelResourceRequest) {
        this.location = location;
        this.name = name;
        this.parallelResourceRequest = parallelResourceRequest;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isBuild() {
        return build;
    }

    public List<CloudResource> getNetworkResources() {
        return new ArrayList<>(networkResources);
    }

    public String getName() {
        return name;
    }

    public int getParallelResourceRequest() {
        return parallelResourceRequest;
    }

    public void addNetworkResources(Collection<CloudResource> resources) {
        networkResources.addAll(resources);
    }

    public List<CloudResource> getGroupResources(String groupName) {
        return groupResources.get(groupName);
    }

    public synchronized void addGroupResources(String groupName, Collection<CloudResource> resources) {
        List<CloudResource> list = groupResources.computeIfAbsent(groupName, k -> new ArrayList<>());
        list.addAll(resources.stream().filter(cloudResource -> groupName.equals(cloudResource.getGroup())).collect(Collectors.toList()));
    }

    public synchronized void addComputeResources(Long index, Collection<CloudResource> resources) {
        List<CloudResource> list = computeResources.computeIfAbsent(index, k -> new ArrayList<>());
        list.addAll(resources);
    }

    public List<CloudResource> getComputeResources(Long index) {
        return computeResources.get(index);
    }

    public synchronized void addLoadBalancerResources(LoadBalancerType type, Collection<CloudResource> resources) {
        List<CloudResource> list = loadBalancerResources.computeIfAbsent(type, k -> new ArrayList<>());
        list.addAll(resources);
    }

    public List<CloudResource> getLoadBalancerResources(LoadBalancerType type) {
        return loadBalancerResources.get(type);
    }

}
