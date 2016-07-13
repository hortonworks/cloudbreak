package com.sequenceiq.cloudbreak.cloud.template.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

public class ResourceBuilderContext extends DynamicModel {

    private Location location;
    private String name;
    private int parallelResourceRequest;
    private Queue<CloudResource> networkResources = new ConcurrentLinkedQueue<>();
    private Map<String, List<CloudResource>> groupResources = new HashMap<>();
    private Map<Long, List<CloudResource>> computeResources = new HashMap<>();
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

    public void addNetworkResources(List<CloudResource> resources) {
        this.networkResources.addAll(resources);
    }

    public List<CloudResource> getGroupResources(String groupName) {
        return groupResources.get(groupName);
    }

    public synchronized void addGroupResources(String groupName, List<CloudResource> resources) {
        List<CloudResource> list = groupResources.get(groupName);
        if (list == null) {
            list = new ArrayList<>();
            groupResources.put(groupName, list);
        }
        list.addAll(resources);
    }

    public synchronized void addComputeResources(long index, List<CloudResource> resources) {
        List<CloudResource> list = computeResources.get(index);
        if (list == null) {
            list = new ArrayList<>();
            computeResources.put(index, list);
        }
        list.addAll(resources);
    }

    public List<CloudResource> getComputeResources(long index) {
        return computeResources.get(index);
    }

}
