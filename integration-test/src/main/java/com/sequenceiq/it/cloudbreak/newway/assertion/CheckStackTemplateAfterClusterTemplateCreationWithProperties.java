package com.sequenceiq.it.cloudbreak.newway.assertion;

import static com.google.common.collect.Sets.newHashSet;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.mpack.ManagementPackDetailsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.ImageCatalogEntity;
import com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.MockCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterTemplateEntity;

public class CheckStackTemplateAfterClusterTemplateCreationWithProperties implements AssertionV2<ClusterTemplateEntity> {

    @Override
    public ClusterTemplateEntity doAssertion(TestContext testContext, ClusterTemplateEntity entity, CloudbreakClient client) {
        ClusterTemplateEntity clusterTemplate = testContext.get(ClusterTemplateEntity.class);
        Optional<ClusterTemplateV4Response> first = entity.getResponses().stream().filter(ct -> ct.getName().equals(clusterTemplate.getName())).findFirst();
        if (!first.isPresent()) {
            throw new IllegalArgumentException("No element in the result");
        }

        ClusterTemplateV4Response clusterTemplateV4Response = first.get();

        StackV4Request stackTemplate = clusterTemplateV4Response.getStackTemplate();
        if (stackTemplate == null) {
            throw new IllegalArgumentException("Stack template is empty");
        }

        if (!"10.10.0.0/16".equals(stackTemplate.getNetwork().getSubnetCIDR())) {
            throw new IllegalArgumentException("SubnetCIDR is mismatch!");
        }

        InstanceGroupV4Request master = getInstanceGroup(stackTemplate.getInstanceGroups(), HostGroupType.MASTER);
        if (!master.getRecipeNames().equals(newHashSet("mock-test-recipe"))) {
            throw new IllegalArgumentException("Master recipes are mismatches!");
        }
        if (!master.getSecurityGroup().getSecurityGroupIds().equals(newHashSet("scgId1", "scgId2"))) {
            throw new IllegalArgumentException("Security groups are mismatches!");
        }

        InstanceGroupV4Request worker = getInstanceGroup(stackTemplate.getInstanceGroups(), HostGroupType.WORKER);
        SecurityRuleV4Request securityRuleRequest = worker.getSecurityGroup().getSecurityRules().get(0);
        if (!Arrays.asList("55", "66", "77").equals(securityRuleRequest.getPorts())) {
            throw new IllegalArgumentException("Ports are mismatches!");
        }

        if (!"ftp".equals(securityRuleRequest.getProtocol())) {
            throw new IllegalArgumentException("Protocol is mismatches!");
        }

        if (!"10.0.0.0/32".equals(securityRuleRequest.getSubnet())) {
            throw new IllegalArgumentException("Subnet is mismatches!");
        }

        if (!stackTemplate.getCluster().getDatabases().equals(newHashSet("mock-test-rds"))) {
            throw new IllegalArgumentException("RDS is mismatch!");
        }

        if (!"mock-test-ldap".equals(stackTemplate.getCluster().getLdapName())) {
            throw new IllegalArgumentException("LDAP is mismatch!");
        }

        if (!"custom-tag".equals(stackTemplate.getTags().getUserDefined().get("some-tag"))) {
            throw new IllegalArgumentException("User defined tag is mismatch!");
        }

        if (!"custom-input".equals(stackTemplate.getInputs().get("some-input"))) {
            throw new IllegalArgumentException("Input is mismatch!");
        }

        if (!"f6e778fc-7f17-4535-9021-515351df3691".equals(stackTemplate.getImage().getId())) {
            throw new IllegalArgumentException("Image ID is mismatch!");
        }

        ImageCatalogEntity imageCatalogEntity = testContext.get(ImageCatalogEntity.class);
        if (!imageCatalogEntity.getName().equals(stackTemplate.getImage().getCatalog())) {
            throw new IllegalArgumentException("Image catalog name is mismatch!");
        }

        if (!MockCloudProvider.CLUSTER_DEFINITION_DEFAULT_NAME.equals(stackTemplate.getCluster().getAmbari().getClusterDefinitionName())) {
            throw new IllegalArgumentException("Blueprint name is mismatch!");
        }

        if (!"2.7.2.2".equals(stackTemplate.getCluster().getAmbari().getRepository().getVersion())) {
            throw new IllegalArgumentException("ambari repo version is mismatch!");
        }

        if (!"http://public-repo-1.hortonworks.com/ambari/centos7/2.x/updates/2.7.2.2"
                .equals(stackTemplate.getCluster().getAmbari().getRepository().getBaseUrl())) {
            throw new IllegalArgumentException("ambari repo base url is mismatch!");
        }

        if (!"http://public-repo-1.hortonworks.com/ambari/centos7/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
                .equals(stackTemplate.getCluster().getAmbari().getRepository().getGpgKeyUrl())) {
            throw new IllegalArgumentException("ambari repo gpg key is mismatch!");
        }

        if (!"2.7".equals(stackTemplate.getCluster().getAmbari().getStackRepository().getVersion())) {
            throw new IllegalArgumentException("stack repo version is mismatch!");
        }

        if (!"2.7.5.0-292".equals(stackTemplate.getCluster().getAmbari().getStackRepository().getRepository().getVersion())) {
            throw new IllegalArgumentException("stack repo repoVersion is mismatch!");
        }

        if (!"HDP".equals(stackTemplate.getCluster().getAmbari().getStackRepository().getStack())) {
            throw new IllegalArgumentException("stack repo stack is mismatch!");
        }

        if (!"http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.7.5.0/HDP-2.7.5.0-292.xml"
                .equals(stackTemplate.getCluster().getAmbari().getStackRepository().getVersionDefinitionFileUrl())) {
            throw new IllegalArgumentException("stack repo vdf-url is mismatch!");
        }

        if (!StringUtils.isEmpty(stackTemplate.getCluster().getAmbari().getPassword())) {
            throw new IllegalArgumentException("Password should be empty!");
        }

        if (!StringUtils.isEmpty(stackTemplate.getCluster().getAmbari().getUserName())) {
            throw new IllegalArgumentException("Username should be empty!");
        }

        List<ManagementPackDetailsV4Request> mpacks = stackTemplate.getCluster().getAmbari().getStackRepository().getMpacks();

        if (mpacks.isEmpty() || !"mock-test-mpack".equals(mpacks.get(0).getName())) {
            throw new IllegalArgumentException("mpack is mismatch!");
        }

        if (!"proxy-name".equals(stackTemplate.getCluster().getGateway().getTopologies().get(0).getTopologyName())) {
            throw new IllegalArgumentException("topology name is mismatch!");
        }

        if (!Collections.singletonList("AMBARI").equals(stackTemplate.getCluster().getGateway().getTopologies().get(0).getExposedServices())) {
            throw new IllegalArgumentException("expose service name is mismatch!");
        }

        return entity;
    }

    private InstanceGroupV4Request getInstanceGroup(List<InstanceGroupV4Request> instanceGroups, HostGroupType hostGroupType) {
        return instanceGroups
                .stream()
                .filter(ig -> hostGroupType.getName().equals(ig.getName()))
                .findFirst()
                .orElseThrow(() -> new InvalidParameterException("Unable to find valid instancegroup by type"));
    }
}
