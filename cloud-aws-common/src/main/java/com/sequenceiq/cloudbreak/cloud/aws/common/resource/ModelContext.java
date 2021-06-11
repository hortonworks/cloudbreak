package com.sequenceiq.cloudbreak.cloud.aws.common.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.aws.common.efs.AwsEfsFileSystem;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;

public class ModelContext {

    private AuthenticatedContext ac;

    private CloudStack stack;

    private boolean existingVPC;

    private boolean existingIGW;

    private List<String> existingSubnetIds = new ArrayList<>();

    private List<String> existingSubnetCidr = new ArrayList<>();

    private List<String> existingVpcCidr = new ArrayList<>();

    private boolean mapPublicIpOnLaunch;

    private String template;

    private boolean enableInstanceProfile;

    private boolean instanceProfileAvailable;

    private String defaultSubnet;

    private Map<String, String> encryptedAMIByGroupName = new HashMap<>();

    private OutboundInternetTraffic outboundInternetTraffic;

    private List<String> vpcCidrs;

    private List<String> prefixListIds;

    private List<AwsLoadBalancer> loadBalancers;

    private boolean enableEfs;

    private AwsEfsFileSystem efsFileSystem;

    public AuthenticatedContext getAc() {
        return ac;
    }

    public CloudStack getStack() {
        return stack;
    }

    public boolean isExistingVPC() {
        return existingVPC;
    }

    public boolean isExistingIGW() {
        return existingIGW;
    }

    public List<String> getExistingSubnetIds() {
        return existingSubnetIds;
    }

    public List<String> getExistingSubnetCidr() {
        return existingSubnetCidr;
    }

    public List<String> getExistingVpcCidr() {
        return existingVpcCidr;
    }

    public boolean isMapPublicIpOnLaunch() {
        return mapPublicIpOnLaunch;
    }

    public String getTemplate() {
        return template;
    }

    public boolean isEnableInstanceProfile() {
        return enableInstanceProfile;
    }

    public boolean isInstanceProfileAvailable() {
        return instanceProfileAvailable;
    }

    public String getDefaultSubnet() {
        return defaultSubnet;
    }

    public Map<String, String> getEncryptedAMIByGroupName() {
        return encryptedAMIByGroupName;
    }

    public OutboundInternetTraffic getOutboundInternetTraffic() {
        return outboundInternetTraffic;
    }

    public List<String> getVpcCidrs() {
        return vpcCidrs;
    }

    public List<String> getPrefixListIds() {
        return prefixListIds;
    }

    public List<AwsLoadBalancer> getLoadBalancers() {
        return loadBalancers;
    }

    public boolean isEnableEfs() {
        return enableEfs;
    }

    public AwsEfsFileSystem getEfsFileSystem() {
        return efsFileSystem;
    }

    public ModelContext withAuthenticatedContext(AuthenticatedContext ac) {
        this.ac = ac;
        return this;
    }

    public ModelContext withStack(CloudStack stack) {
        this.stack = stack;
        return this;
    }

    public ModelContext withExistingVpc(boolean existingVpc) {
        existingVPC = existingVpc;
        return this;
    }

    public ModelContext withExistingIGW(boolean existingIGW) {
        this.existingIGW = existingIGW;
        return this;
    }

    public ModelContext withExistingSubnetCidr(List<String> cidr) {
        existingSubnetCidr = cidr;
        return this;
    }

    public ModelContext withExistinVpcCidr(List<String> cidr) {
        existingVpcCidr = cidr;
        return this;
    }

    public ModelContext withExistingSubnetIds(List<String> subnetIds) {
        existingSubnetIds = subnetIds;
        return this;
    }

    public ModelContext mapPublicIpOnLaunch(boolean mapPublicIpOnLaunch) {
        this.mapPublicIpOnLaunch = mapPublicIpOnLaunch;
        return this;
    }

    public ModelContext withEnableInstanceProfile(boolean enableInstanceProfile) {
        this.enableInstanceProfile = enableInstanceProfile;
        return this;
    }

    public ModelContext withInstanceProfileAvailable(boolean instanceProfileAvailable) {
        this.instanceProfileAvailable = instanceProfileAvailable;
        return this;
    }

    public ModelContext withTemplate(String template) {
        this.template = template;
        return this;
    }

    public ModelContext withDefaultSubnet(String subnet) {
        defaultSubnet = subnet;
        return this;
    }

    public ModelContext withOutboundInternetTraffic(OutboundInternetTraffic outboundInternetTraffic) {
        this.outboundInternetTraffic = outboundInternetTraffic;
        return this;
    }

    public ModelContext withVpcCidrs(List<String> vpcCidrs) {
        this.vpcCidrs = vpcCidrs;
        return this;
    }

    public ModelContext withPrefixListIds(List<String> prefixListIds) {
        this.prefixListIds = prefixListIds;
        return this;
    }

    public ModelContext withEncryptedAMIByGroupName(Map<String, String> encryptedAMIByGroupName) {
        this.encryptedAMIByGroupName.putAll(encryptedAMIByGroupName);
        return this;
    }

    public ModelContext withLoadBalancers(List<AwsLoadBalancer> loadBalancers) {
        this.loadBalancers = loadBalancers;
        return this;
    }

    public ModelContext withEnableEfs(boolean enableEfs) {
        this.enableEfs = enableEfs;
        return this;
    }

    public ModelContext withEfsFileSystem(AwsEfsFileSystem efsFileSystem) {
        this.efsFileSystem = efsFileSystem;
        return this;
    }
}
