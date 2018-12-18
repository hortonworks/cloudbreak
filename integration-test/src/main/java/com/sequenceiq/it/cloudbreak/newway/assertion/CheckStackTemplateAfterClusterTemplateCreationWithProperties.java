package com.sequenceiq.it.cloudbreak.newway.assertion;

import static com.google.common.collect.Sets.newHashSet;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.model.SecurityRuleRequest;
import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackDetails;
import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateResponse;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.ImageCatalogEntity;
import com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.MockCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterTemplateEntity;

public class CheckStackTemplateAfterClusterTemplateCreationWithProperties implements AssertionV2<ClusterTemplateEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckStackTemplateAfterClusterTemplateCreationWithProperties.class);

    @Override
    public ClusterTemplateEntity doAssertion(TestContext testContext, ClusterTemplateEntity entity, CloudbreakClient client) throws Exception {
        ClusterTemplateEntity clusterTemplate = testContext.get(ClusterTemplateEntity.class);
        Optional<ClusterTemplateResponse> first = entity.getResponses().stream().filter(ct -> ct.getName().equals(clusterTemplate.getName())).findFirst();
        if (!first.isPresent()) {
            throw new IllegalArgumentException("No element in the result");
        }

        ClusterTemplateResponse clusterTemplateResponse = first.get();

        StackV2Request stackTemplate = clusterTemplateResponse.getStackTemplate();
        if (stackTemplate == null) {
            throw new IllegalArgumentException("Stack template is empty");
        }

        if (!"10.10.0.0/16".equals(stackTemplate.getNetwork().getSubnetCIDR())) {
            throw new IllegalArgumentException("SubnetCIDR is mismatch!");
        }

        if (!"subnet-value".equals(stackTemplate.getNetwork().getParameters().get("customParameter"))) {
            throw new IllegalArgumentException("CustomParameter is mismatch!");
        }

        InstanceGroupV2Request master = getInstanceGroup(stackTemplate.getInstanceGroups(), HostGroupType.MASTER);
        if (!master.getRecipeNames().equals(newHashSet("mock-test-recipe"))) {
            throw new IllegalArgumentException("Master recipes are mismatches!");
        }
        if (!master.getSecurityGroup().getSecurityGroupIds().equals(newHashSet("scgId1", "scgId2"))) {
            throw new IllegalArgumentException("Security groups are mismatches!");
        }

        InstanceGroupV2Request worker = getInstanceGroup(stackTemplate.getInstanceGroups(), HostGroupType.WORKER);
        SecurityRuleRequest securityRuleRequest = worker.getSecurityGroup().getSecurityRules().get(0);
        if (!"55,66,77".equals(securityRuleRequest.getPorts())) {
            throw new IllegalArgumentException("Ports are mismatches!");
        }

        if (!"ftp".equals(securityRuleRequest.getProtocol())) {
            throw new IllegalArgumentException("Protocol is mismatches!");
        }

        if (!"10.0.0.0/32".equals(securityRuleRequest.getSubnet())) {
            throw new IllegalArgumentException("Subnet is mismatches!");
        }

        if (!stackTemplate.getCluster().getRdsConfigNames().equals(newHashSet("mock-test-rds"))) {
            throw new IllegalArgumentException("RDS is mismatch!");
        }

        if (!"mock-test-ldap".equals(stackTemplate.getCluster().getLdapConfigName())) {
            throw new IllegalArgumentException("LDAP is mismatch!");
        }

        if (!"custom-tag".equals(stackTemplate.getTags().getUserDefinedTags().get("some-tag"))) {
            throw new IllegalArgumentException("User defined tag is mismatch!");
        }

        if (!"custom-input".equals(stackTemplate.getInputs().get("some-input"))) {
            throw new IllegalArgumentException("Input is mismatch!");
        }

        if (!"f6e778fc-7f17-4535-9021-515351df3691".equals(stackTemplate.getImageSettings().getImageId())) {
            throw new IllegalArgumentException("Image ID is mismatch!");
        }

        ImageCatalogEntity imageCatalogEntity = testContext.get(ImageCatalogEntity.class);
        if (!imageCatalogEntity.getName().equals(stackTemplate.getImageSettings().getImageCatalog())) {
            throw new IllegalArgumentException("Image catalog name is mismatch!");
        }

        if (!MockCloudProvider.BLUEPRINT_DEFAULT_NAME.equals(stackTemplate.getCluster().getAmbari().getBlueprintName())) {
            throw new IllegalArgumentException("Blueprint name is mismatch!");
        }

        if (!"2.6.2.2".equals(stackTemplate.getCluster().getAmbari().getAmbariRepoDetailsJson().getVersion())) {
            throw new IllegalArgumentException("ambari repo version is mismatch!");
        }

        if (!"http://public-repo-1.hortonworks.com/ambari/centos7/2.x/updates/2.6.2.2"
                .equals(stackTemplate.getCluster().getAmbari().getAmbariRepoDetailsJson().getBaseUrl())) {
            throw new IllegalArgumentException("ambari repo base url is mismatch!");
        }

        if (!"http://public-repo-1.hortonworks.com/ambari/centos7/RPM-GPG-KEY/RPM-GPG-KEY-Jenkins"
                .equals(stackTemplate.getCluster().getAmbari().getAmbariRepoDetailsJson().getGpgKeyUrl())) {
            throw new IllegalArgumentException("ambari repo gpg key is mismatch!");
        }

        if (!"2.6".equals(stackTemplate.getCluster().getAmbari().getAmbariStackDetails().getVersion())) {
            throw new IllegalArgumentException("ambari repo version is mismatch!");
        }

        if (!"2.6.5.0-292".equals(stackTemplate.getCluster().getAmbari().getAmbariStackDetails().getRepositoryVersion())) {
            throw new IllegalArgumentException("ambari repo version is mismatch!");
        }

        if (!"HDP".equals(stackTemplate.getCluster().getAmbari().getAmbariStackDetails().getStack())) {
            throw new IllegalArgumentException("ambari repo base url is mismatch!");
        }

        if (!"http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.5.0/HDP-2.6.5.0-292.xml"
                .equals(stackTemplate.getCluster().getAmbari().getAmbariStackDetails().getVersionDefinitionFileUrl())) {
            throw new IllegalArgumentException("ambari repo gpg key is mismatch!");
        }

        if (!"some-value".equals(stackTemplate.getParameters().get("param1"))) {
            throw new IllegalArgumentException("parameter is mismatch!");
        }

        if (!StringUtils.isEmpty(stackTemplate.getCluster().getAmbari().getPassword())) {
            throw new IllegalArgumentException("Password should be empty!");
        }

        if (!StringUtils.isEmpty(stackTemplate.getCluster().getAmbari().getUserName())) {
            throw new IllegalArgumentException("Username should be empty!");
        }

        List<ManagementPackDetails> mpacks = stackTemplate.getCluster().getAmbari().getAmbariStackDetails().getMpacks();

        if (mpacks.isEmpty() || !"mock-test-mpack".equals(mpacks.get(0).getName())) {
            throw new IllegalArgumentException("mpack is mismatch!");
        }

        if (!"proxy-name".equals(stackTemplate.getCluster().getAmbari().getGateway().getTopologies().get(0).getTopologyName())) {
            throw new IllegalArgumentException("topology name is mismatch!");
        }

        if (!Collections.singletonList("AMBARI").equals(stackTemplate.getCluster().getAmbari().getGateway().getTopologies().get(0).getExposedServices())) {
            throw new IllegalArgumentException("expose service name is mismatch!");
        }

        return entity;
    }

    private InstanceGroupV2Request getInstanceGroup(List<InstanceGroupV2Request> instanceGroups, HostGroupType hostGroupType) {
        return instanceGroups.stream().filter(ig -> hostGroupType.getName().equals(ig.getGroup())).findFirst().get();
    }
}
