package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.Collection;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.aws.common.efs.AwsEfsFileSystem;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsGroupView;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;

public class AwsNativeModel {

    private Collection<AwsGroupView> instanceGroups;

    private Collection<AwsGroupView> gatewayGroups;

    private Boolean existingVPC;

    private Boolean existingIGW;

    private Boolean existingSubnet;

    private Boolean enableInstanceProfile;

    private Boolean existingRole;

    private List<String> cbSubnet;

    private List<String> vpcSubnet;

    private boolean dedicatedInstances;

    private boolean availabilitySetNeeded;

    private boolean mapPublicIpOnLaunch;

    private OutboundInternetTraffic outboundInternetTraffic;

    private List<String> vpcCidrs;

    private List<String> prefixListIds;

    private List<AwsLoadBalancer> loadBalancers;

    private boolean enableEfs;

    private AwsEfsFileSystem efsFileSystem;

    public Collection<AwsGroupView> getInstanceGroups() {
        return instanceGroups;
    }

    public void setInstanceGroups(Collection<AwsGroupView> instanceGroups) {
        this.instanceGroups = instanceGroups;
    }

    public Collection<AwsGroupView> getGatewayGroups() {
        return gatewayGroups;
    }

    public void setGatewayGroups(Collection<AwsGroupView> gatewayGroups) {
        this.gatewayGroups = gatewayGroups;
    }

    public Boolean getExistingVPC() {
        return existingVPC;
    }

    public void setExistingVPC(Boolean existingVPC) {
        this.existingVPC = existingVPC;
    }

    public Boolean getExistingIGW() {
        return existingIGW;
    }

    public void setExistingIGW(Boolean existingIGW) {
        this.existingIGW = existingIGW;
    }

    public Boolean getExistingSubnet() {
        return existingSubnet;
    }

    public void setExistingSubnet(Boolean existingSubnet) {
        this.existingSubnet = existingSubnet;
    }

    public Boolean getEnableInstanceProfile() {
        return enableInstanceProfile;
    }

    public void setEnableInstanceProfile(Boolean enableInstanceProfile) {
        this.enableInstanceProfile = enableInstanceProfile;
    }

    public Boolean getExistingRole() {
        return existingRole;
    }

    public void setExistingRole(Boolean existingRole) {
        this.existingRole = existingRole;
    }

    public List<String> getCbSubnet() {
        return cbSubnet;
    }

    public void setCbSubnet(List<String> cbSubnet) {
        this.cbSubnet = cbSubnet;
    }

    public List<String> getVpcSubnet() {
        return vpcSubnet;
    }

    public void setVpcSubnet(List<String> vpcSubnet) {
        this.vpcSubnet = vpcSubnet;
    }

    public boolean isDedicatedInstances() {
        return dedicatedInstances;
    }

    public void setDedicatedInstances(boolean dedicatedInstances) {
        this.dedicatedInstances = dedicatedInstances;
    }

    public boolean isAvailabilitySetNeeded() {
        return availabilitySetNeeded;
    }

    public void setAvailabilitySetNeeded(boolean availabilitySetNeeded) {
        this.availabilitySetNeeded = availabilitySetNeeded;
    }

    public boolean isMapPublicIpOnLaunch() {
        return mapPublicIpOnLaunch;
    }

    public void setMapPublicIpOnLaunch(boolean mapPublicIpOnLaunch) {
        this.mapPublicIpOnLaunch = mapPublicIpOnLaunch;
    }

    public OutboundInternetTraffic getOutboundInternetTraffic() {
        return outboundInternetTraffic;
    }

    public void setOutboundInternetTraffic(OutboundInternetTraffic outboundInternetTraffic) {
        this.outboundInternetTraffic = outboundInternetTraffic;
    }

    public List<String> getVpcCidrs() {
        return vpcCidrs;
    }

    public void setVpcCidrs(List<String> vpcCidrs) {
        this.vpcCidrs = vpcCidrs;
    }

    public List<String> getPrefixListIds() {
        return prefixListIds;
    }

    public void setPrefixListIds(List<String> prefixListIds) {
        this.prefixListIds = prefixListIds;
    }

    public List<AwsLoadBalancer> getLoadBalancers() {
        return loadBalancers;
    }

    public void setLoadBalancers(List<AwsLoadBalancer> loadBalancers) {
        this.loadBalancers = loadBalancers;
    }

    public boolean isEnableEfs() {
        return enableEfs;
    }

    public void setEnableEfs(boolean enableEfs) {
        this.enableEfs = enableEfs;
    }

    public AwsEfsFileSystem getEfsFileSystem() {
        return efsFileSystem;
    }

    public void setEfsFileSystem(AwsEfsFileSystem efsFileSystem) {
        this.efsFileSystem = efsFileSystem;
    }

}
