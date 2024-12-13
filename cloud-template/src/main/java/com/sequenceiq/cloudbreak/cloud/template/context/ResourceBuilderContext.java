package com.sequenceiq.cloudbreak.cloud.template.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;
import com.sequenceiq.common.api.type.LoadBalancerType;

public class ResourceBuilderContext extends DynamicModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceBuilderContext.class);

    private final Location location;

    private final String name;

    private final int parallelResourceRequest;

    private final Queue<CloudResource> networkResources = new ConcurrentLinkedQueue<>();

    private final Queue<CloudResource> authenticationResources = new ConcurrentLinkedQueue<>();

    private final Map<String, List<CloudResource>> groupResources = new HashMap<>();

    private final Map<Long, List<CloudResource>> computeResources = new HashMap<>();

    private final Map<LoadBalancerType, List<CloudResource>> loadBalancerResources = new HashMap<>();

    private final Lock lock = new ReentrantLock();

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

    public void setBuild(boolean build) {
        this.build = build;
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

    public void addAuthenticationResources(Collection<CloudResource> resources) {
        authenticationResources.addAll(resources);
    }

    public List<CloudResource> getGroupResources(String groupName) {
        return groupResources.get(groupName);
    }

    public void addGroupResources(String groupName, Collection<CloudResource> resources) {
        withLock(() -> {
            List<CloudResource> list = groupResources.computeIfAbsent(groupName, k -> new ArrayList<>());
            list.addAll(resources.stream().filter(cloudResource -> groupName.equals(cloudResource.getGroup())).toList());
        });
    }

    public void addComputeResources(Long privateId, Collection<CloudResource> resources) {
        withLock(() -> {
            List<CloudResource> list = computeResources.computeIfAbsent(privateId, k -> new ArrayList<>());
            for (CloudResource resource : resources) {
                if (list.contains(resource)) {
                    LOGGER.info("Resource {} already exists for private id {}", resource, privateId);
                } else {
                    list.add(resource);
                }
            }
        });
    }

    public List<CloudResource> getComputeResources(Long privateId) {
        return computeResources.get(privateId);
    }

    public void addLoadBalancerResources(LoadBalancerType type, Collection<CloudResource> resources) {
        withLock(() -> {
            List<CloudResource> list = loadBalancerResources.computeIfAbsent(type, k -> new ArrayList<>());
            list.addAll(resources);
        });
    }

    public List<CloudResource> getLoadBalancerResources(LoadBalancerType type) {
        return loadBalancerResources.get(type);
    }

    private void withLock(Runnable runnable) {
        try {
            lock.lock();
            runnable.run();
        } finally {
            lock.unlock();
        }
    }

}
