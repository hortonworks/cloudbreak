package com.sequenceiq.cloudbreak.blueprint.kerberos;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.InstanceStatus;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.JacksonBlueprintProcessor;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@RunWith(MockitoJUnitRunner.class)
public class KerberosBlueprintServiceTest {

    @Spy
    private AmbariClient ambariClient;

    @Spy
    private JacksonBlueprintProcessor blueprintProcessor;

    @Mock
    private BlueprintPreparationObject blueprintPreparationObject;

    @Spy
    private KerberosDetailService kerberosDetailService;

    @InjectMocks
    private KerberosBlueprintService underTest;

    private final AmbariClient kerberosService = new AmbariClient();

    @Test
    public void testExtendBlueprintWithKerberosOnKerberizedBlueprint() throws IOException {
        String expectedBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/bp-kerberized-test.bp");
        Blueprint blueprint = TestUtil.blueprint("name", expectedBlueprint);
        Cluster cluster = TestUtil.cluster(blueprint, blueprintPreparationObject.getStack(), 1L, TestUtil.kerberosConfig());
        BlueprintPreparationObject object = BlueprintPreparationObject.Builder.builder()
                .withStack(stack())
                .withAmbariClient(kerberosService)
                .withCluster(cluster)
                .build();

        String actualBlueprint = underTest.customTextManipulation(object, blueprint.getBlueprintText());

        JsonNode expectedNode = JsonUtil.readTree(expectedBlueprint);
        JsonNode resultNode = JsonUtil.readTree(actualBlueprint);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testExtendBlueprintWithKerberosManagedKerberos() throws IOException {
        String blueprintText = FileReaderUtils.readFileFromClasspath("blueprints/bp-not-kerberized.bp");
        Blueprint blueprint = TestUtil.blueprint("name", blueprintText);
        Cluster cluster = TestUtil.cluster(blueprint, blueprintPreparationObject.getStack(), 1L, TestUtil.kerberosConfig());
        BlueprintPreparationObject object = BlueprintPreparationObject.Builder.builder()
                .withStack(stack())
                .withAmbariClient(kerberosService)
                .withCluster(cluster)
                .build();

        String actualBlueprint = underTest.customTextManipulation(object, blueprint.getBlueprintText());

        String expectedBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/bp-not-kerberized-cloudbreak-managed-expected.bp");

        JsonNode expectedNode = JsonUtil.readTree(expectedBlueprint);
        JsonNode resultNode = JsonUtil.readTree(actualBlueprint);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testExtendBlueprintWithKerberosCustomConfig() throws IOException {
        String blueprintText = FileReaderUtils.readFileFromClasspath("blueprints/bp-not-kerberized.bp");
        Blueprint blueprint = TestUtil.blueprint("name", blueprintText);
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setDescriptor("{\"kerberos-env\":{\"properties\":{\"install_packages\":false,\"realm\":\"REALM.BP\",\"kdc_type\":\"mit-kdc\","
            + "\"kdc_hosts\":\"kdc_host.bp\",\"admin_server_host\":\"admin_server_host.bp\",\"encryption_types\":\"enc_types.bp\",\"ldap_url\":\"\","
            + "\"container_dn\":\"\"}}}");
        kerberosConfig.setKrb5Conf("{\"krb5-conf\":{\"properties\":{\"domains\":\".domains.bp\",\"manage_krb5_conf\":\"true\",\"content\":\"content.bp\"}}}");
        kerberosConfig.setTcpAllowed(true);
        Cluster cluster = TestUtil.cluster(blueprint, blueprintPreparationObject.getStack(), 1L, kerberosConfig);
        BlueprintPreparationObject object = BlueprintPreparationObject.Builder.builder()
                .withStack(stack())
                .withAmbariClient(kerberosService)
                .withCluster(cluster)
                .build();

        String actualBlueprint = underTest.customTextManipulation(object, blueprint.getBlueprintText());

        String expectedBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/bp-not-kerberized-custom-config-expected.bp");

        JsonNode expectedNode = JsonUtil.readTree(expectedBlueprint);
        JsonNode resultNode = JsonUtil.readTree(actualBlueprint);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testExtendBlueprintWithKerberosExisting() throws IOException {
        String blueprintText = FileReaderUtils.readFileFromClasspath("blueprints/bp-not-kerberized.bp");
        Blueprint blueprint = TestUtil.blueprint("name", blueprintText);
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setPrincipal("principal.conf");
        kerberosConfig.setPrincipal("passwd.conf");
        kerberosConfig.setUrl("url.conf");
        kerberosConfig.setAdminUrl("adminUrl.conf");
        kerberosConfig.setRealm("realm.conf");
        kerberosConfig.setLdapUrl("ldapUrl.conf");
        kerberosConfig.setContainerDn("containerDn.conf");
        kerberosConfig.setTcpAllowed(true);
        Cluster cluster = TestUtil.cluster(blueprint, blueprintPreparationObject.getStack(), 1L, kerberosConfig);
        BlueprintPreparationObject object = BlueprintPreparationObject.Builder.builder()
                .withStack(stack())
                .withAmbariClient(kerberosService)
                .withCluster(cluster)
                .build();
        String actualBlueprint = underTest.customTextManipulation(object, blueprint.getBlueprintText());

        String expectedBlueprint = FileReaderUtils.readFileFromClasspath("blueprints/bp-not-kerberized-existing-expected.bp");

        JsonNode expectedNode = JsonUtil.readTree(expectedBlueprint);
        JsonNode resultNode = JsonUtil.readTree(actualBlueprint);
        Assert.assertEquals(expectedNode, resultNode);
    }

    private Stack stack() {
        Stack stack = TestUtil.stack();
        InstanceGroup instanceGroup = TestUtil.instanceGroup(1L, InstanceGroupType.GATEWAY, null);
        InstanceMetaData instanceMetaData = TestUtil
                .primaryGateWayInstanceMetaData(1L, 1L, InstanceStatus.REGISTERED, true, instanceGroup);
        instanceGroup.setInstanceMetaData(Sets.newHashSet(instanceMetaData));
        stack.setInstanceGroups(Sets.newHashSet(instanceGroup));
        return stack;
    }
}