package com.sequenceiq.cloudbreak.blueprint.kerberos;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.blueprint.filesystem.BlueprintTestUtil;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.VaultService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@RunWith(MockitoJUnitRunner.class)
public class KerberosBlueprintServiceTest {

    @Spy
    private BlueprintProcessorFactory blueprintProcessorFactory;

    @Mock
    private TemplatePreparationObject templatePreparationObject;

    @Mock
    private VaultService vaultService;

    @Spy
    private KerberosDetailService kerberosDetailService;

    @InjectMocks
    private KerberosBlueprintService underTest;

    @Test
    public void testExtendBlueprintWithKerberosOnKerberizedBlueprint() throws IOException {
        String expectedBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        Blueprint blueprint = TestUtil.blueprint("name", expectedBlueprint);
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster(blueprint, stack, 1L, TestUtil.kerberosConfig());
        TemplatePreparationObject object = Builder.builder()
                .withKerberosConfig(cluster.getKerberosConfig())
                .withGeneralClusterConfigs(BlueprintTestUtil.generalClusterConfigs())
                .build();

        BlueprintTextProcessor b = new BlueprintTextProcessor(blueprint.getBlueprintText());
        String actualBlueprint = underTest.customTextManipulation(object, b).asText();

        JsonNode expectedNode = JsonUtil.readTree(expectedBlueprint);
        JsonNode resultNode = JsonUtil.readTree(actualBlueprint);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testExtendBlueprintWithKerberosManagedKerberos() throws IOException {
        String blueprintText = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-not-kerberized.bp");

        Blueprint blueprint = TestUtil.blueprint("name", blueprintText);
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster(blueprint, stack, 1L, TestUtil.kerberosConfig());
        GeneralClusterConfigs generalClusterConfigs = BlueprintTestUtil.generalClusterConfigs();
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.of("test-1-1"));
        generalClusterConfigs.setGatewayInstanceMetadataPresented(false);

        TemplatePreparationObject object = Builder.builder()
                .withKerberosConfig(cluster.getKerberosConfig())
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();

        BlueprintTextProcessor b = new BlueprintTextProcessor(blueprint.getBlueprintText());
        String actualBlueprint = underTest.customTextManipulation(object, b).asText();

        String expectedBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-not-kerberized-cloudbreak-managed-expected.bp");

        JsonNode expectedNode = JsonUtil.readTree(expectedBlueprint);
        JsonNode resultNode = JsonUtil.readTree(actualBlueprint);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testExtendBlueprintWithKerberosCustomConfig() throws IOException {
        String blueprintText = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-not-kerberized.bp");

        Blueprint blueprint = TestUtil.blueprint("name", blueprintText);
        Stack stack = TestUtil.stack();
        String descriptorPath = "secret/descriptor";
        String krb5Path = "secret/krb5conf";
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setDescriptor(descriptorPath);
        kerberosConfig.setKrb5Conf(krb5Path);
        kerberosConfig.setTcpAllowed(true);
        Cluster cluster = TestUtil.cluster(blueprint, stack, 1L, kerberosConfig);
        TemplatePreparationObject object = Builder.builder()
                .withKerberosConfig(cluster.getKerberosConfig())
                .withGeneralClusterConfigs(BlueprintTestUtil.generalClusterConfigs())
                .build();
        when(vaultService.resolveSingleValue(descriptorPath)).thenReturn("{\"kerberos-env\":{\"properties\":"
                + "{\"install_packages\":false,\"realm\":\"REALM.BP\",\"kdc_type\":\"mit-kdc\","
                + "\"kdc_hosts\":\"kdc_host.bp\",\"admin_server_host\":\"admin_server_host.bp\",\"encryption_types\":\"enc_types.bp\",\"ldap_url\":\"\","
                + "\"container_dn\":\"\"}}}");
        when(vaultService.resolveSingleValue(krb5Path)).thenReturn("{\"krb5-conf\":{\"properties\":{\"domains\":\".domains.bp\","
                + "\"manage_krb5_conf\":\"true\",\"content\":\"content.bp\"}}}");

        BlueprintTextProcessor b = new BlueprintTextProcessor(blueprint.getBlueprintText());
        String actualBlueprint = underTest.customTextManipulation(object, b).asText();

        String expectedBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-not-kerberized-custom-config-expected.bp");

        JsonNode expectedNode = JsonUtil.readTree(expectedBlueprint);
        JsonNode resultNode = JsonUtil.readTree(actualBlueprint);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testExtendBlueprintWithKerberosExisting() throws IOException {
        String blueprintText = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-not-kerberized.bp");

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
        Stack stack = TestUtil.stack();

        GeneralClusterConfigs generalClusterConfigs = BlueprintTestUtil.generalClusterConfigs();
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.of("test-1-1"));
        generalClusterConfigs.setGatewayInstanceMetadataPresented(false);

        Cluster cluster = TestUtil.cluster(blueprint, stack, 1L, kerberosConfig);
        TemplatePreparationObject object = Builder.builder()
                .withKerberosConfig(cluster.getKerberosConfig())
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
        BlueprintTextProcessor b = new BlueprintTextProcessor(blueprint.getBlueprintText());
        String actualBlueprint = underTest.customTextManipulation(object, b).asText();

        String expectedBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-not-kerberized-existing-expected.bp");

        JsonNode expectedNode = JsonUtil.readTree(expectedBlueprint);
        JsonNode resultNode = JsonUtil.readTree(actualBlueprint);
        Assert.assertEquals(expectedNode, resultNode);
    }
}