package com.sequenceiq.cloudbreak.service.stack;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.StackDto;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayDto;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

public class StackProxy {

    private StackDto stack;

    private ClusterDto cluster;

    private Network network;

    private Workspace workspace;

    private Map<InstanceGroupDto, List<InstanceMetadataDto>> instanceGroups;

    private Set<Resource> resources;

    private Blueprint blueprint;

    private GatewayDto gateway;

    private Orchestrator orchestrator;

    private FileSystem fileSystem;

    private FileSystem additionalFileSystem;

    public Set<ClusterComponent> getComponents() {
        return components;
    }

    private Set<ClusterComponent> components;

    public StackProxy(StackDto stack, ClusterDto cluster, Network network, Workspace workspace, Map<InstanceGroupDto, List<InstanceMetadataDto>> instanceGroups, Set<Resource> resources, Blueprint blueprint, GatewayDto gateway, Orchestrator orchestrator, FileSystem fileSystem, FileSystem additionalFileSystem, Set<ClusterComponent> components) {
        this.stack = stack;
        this.cluster = cluster;
        this.network = network;
        this.workspace = workspace;
        this.instanceGroups = instanceGroups;
        this.resources = resources;
        this.blueprint = blueprint;
        this.gateway = gateway;
        this.orchestrator = orchestrator;
        this.fileSystem = fileSystem;
        this.additionalFileSystem = additionalFileSystem;
        this.components = components;
    }

    public StackDto getStack() {
        return stack;
    }

    public Map<InstanceGroupDto, List<InstanceMetadataDto>> getInstanceGroups() {
        return instanceGroups;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public ClusterDto getCluster() {
        return cluster;
    }

    public Network getNetwork() {
        return network;
    }

    public Set<Resource> getResources() {
        return resources;
    }

    public Blueprint getBlueprint() {
        return blueprint;
    }

    public GatewayDto getGateway() {
        return gateway;
    }

    public Orchestrator getOrchestrator() {
        return orchestrator;
    }

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    public FileSystem getAdditionalFileSystem() {
        return additionalFileSystem;
    }

}
