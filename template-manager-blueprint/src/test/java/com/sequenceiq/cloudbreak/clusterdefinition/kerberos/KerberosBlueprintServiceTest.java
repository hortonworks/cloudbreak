package com.sequenceiq.cloudbreak.clusterdefinition.kerberos;

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
import com.sequenceiq.cloudbreak.clusterdefinition.AmbariBlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.clusterdefinition.filesystem.ClusterDefinitionTestUtil;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.type.KerberosType;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@RunWith(MockitoJUnitRunner.class)
public class KerberosBlueprintServiceTest {

    @Spy
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    @Mock
    private TemplatePreparationObject templatePreparationObject;

    @Spy
    private KerberosDetailService kerberosDetailService;

    @InjectMocks
    private KerberosClusterDefinitionService underTest;

    @Test
    public void testExtendBlueprintWithKerberosOnKerberizedBlueprint() throws IOException {
        String expectedBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        ClusterDefinition clusterDefinition = TestUtil.clusterDefinition("name", expectedBlueprint);
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster(clusterDefinition, stack, 1L, TestUtil.kerberosConfigFreeipa());
        TemplatePreparationObject object = Builder.builder()
                .withKerberosConfig(cluster.getKerberosConfig())
                .withGeneralClusterConfigs(ClusterDefinitionTestUtil.generalClusterConfigs())
                .build();

        AmbariBlueprintTextProcessor b = new AmbariBlueprintTextProcessor(clusterDefinition.getClusterDefinitionText());
        String actualBlueprint = underTest.customTextManipulation(object, b).asText();

        JsonNode expectedNode = JsonUtil.readTree(expectedBlueprint);
        JsonNode resultNode = JsonUtil.readTree(actualBlueprint);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testExtendBlueprintWithKerberosCustomConfig() throws IOException {
        String blueprintText = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-not-kerberized.bp");

        ClusterDefinition clusterDefinition = TestUtil.clusterDefinition("name", blueprintText);
        Stack stack = TestUtil.stack();
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setDescriptor("{\"kerberos-env\":{\"properties\":"
                + "{\"install_packages\":false,\"realm\":\"REALM.BP\",\"kdc_type\":\"mit-kdc\","
                + "\"kdc_hosts\":\"kdc_host.bp\",\"admin_server_host\":\"admin_server_host.bp\",\"encryption_types\":\"enc_types.bp\",\"ldap_url\":\"\","
                + "\"container_dn\":\"\"}}}");
        kerberosConfig.setKrb5Conf("{\"krb5-conf\":{\"properties\":{\"domains\":\".domains.bp\","
                + "\"manage_krb5_conf\":\"true\",\"content\":\"content.bp\"}}}");
        kerberosConfig.setTcpAllowed(true);
        kerberosConfig.setType(KerberosType.MIT);
        Cluster cluster = TestUtil.cluster(clusterDefinition, stack, 1L, kerberosConfig);
        TemplatePreparationObject object = Builder.builder()
                .withKerberosConfig(cluster.getKerberosConfig())
                .withGeneralClusterConfigs(ClusterDefinitionTestUtil.generalClusterConfigs())
                .build();

        AmbariBlueprintTextProcessor b = new AmbariBlueprintTextProcessor(clusterDefinition.getClusterDefinitionText());
        String actualBlueprint = underTest.customTextManipulation(object, b).asText();

        String expectedBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-not-kerberized-custom-config-expected.bp");

        JsonNode expectedNode = JsonUtil.readTree(expectedBlueprint);
        JsonNode resultNode = JsonUtil.readTree(actualBlueprint);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testExtendBlueprintWithKerberosExisting() throws IOException {
        String blueprintText = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-not-kerberized.bp");

        ClusterDefinition clusterDefinition = TestUtil.clusterDefinition("name", blueprintText);
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setPrincipal("principal.conf");
        kerberosConfig.setPrincipal("passwd.conf");
        kerberosConfig.setUrl("url.conf");
        kerberosConfig.setAdminUrl("adminUrl.conf");
        kerberosConfig.setRealm("realm.conf");
        kerberosConfig.setLdapUrl("ldapUrl.conf");
        kerberosConfig.setContainerDn("containerDn.conf");
        kerberosConfig.setTcpAllowed(true);
        kerberosConfig.setType(KerberosType.ACTIVE_DIRECTORY);
        Stack stack = TestUtil.stack();

        GeneralClusterConfigs generalClusterConfigs = ClusterDefinitionTestUtil.generalClusterConfigs();
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.of("test-1-1"));
        generalClusterConfigs.setGatewayInstanceMetadataPresented(false);

        Cluster cluster = TestUtil.cluster(clusterDefinition, stack, 1L, kerberosConfig);
        TemplatePreparationObject object = Builder.builder()
                .withKerberosConfig(cluster.getKerberosConfig())
                .withGeneralClusterConfigs(generalClusterConfigs)
                .build();
        AmbariBlueprintTextProcessor b = new AmbariBlueprintTextProcessor(clusterDefinition.getClusterDefinitionText());
        String actualBlueprint = underTest.customTextManipulation(object, b).asText();

        String expectedBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-not-kerberized-existing-expected.bp");

        JsonNode expectedNode = JsonUtil.readTree(expectedBlueprint);
        JsonNode resultNode = JsonUtil.readTree(actualBlueprint);
        Assert.assertEquals(expectedNode, resultNode);
    }
}