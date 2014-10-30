package com.sequenceiq.cloudbreak.service.stack.resource.gcc.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.NetworkInterface;
import com.sequenceiq.cloudbreak.service.stack.resource.ProvisionContextObject;

public class GccProvisionContextObject extends ProvisionContextObject {

    private String projectId;
    private Compute compute;
    private List<NetworkInterface> networkInterfaces = new ArrayList<>();
    private Map<String, List<AttachedDisk>> diskMap = new HashMap<>();
    private String userData;

    public GccProvisionContextObject(Long stackId, String projectId, Compute compute) {
        super(stackId);
        this.projectId = projectId;
        this.compute = compute;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public Compute getCompute() {
        return compute;
    }

    public void setCompute(Compute compute) {
        this.compute = compute;
    }

    public String getUserData() {
        return userData;
    }

    public void setUserData(String userData) {
        this.userData = userData;
    }

    public List<NetworkInterface> getNetworkInterfaces() {
        return networkInterfaces;
    }

    public GccProvisionContextObject withNetworkInterfaces(List<NetworkInterface> networkInterfaces) {
        this.networkInterfaces = networkInterfaces;
        return this;
    }

    public Map<String, List<AttachedDisk>> getDiskMap() {
        return diskMap;
    }

    public List<AttachedDisk> getDiskList(String name) {
        List<AttachedDisk> attachedDisks = diskMap.get(name);
        return attachedDisks == null ? new ArrayList<AttachedDisk>() : attachedDisks;
    }

    public GccProvisionContextObject withDiskList(String name, List<AttachedDisk> diskList) {
        diskMap.put(name, diskList);
        return this;
    }
}
