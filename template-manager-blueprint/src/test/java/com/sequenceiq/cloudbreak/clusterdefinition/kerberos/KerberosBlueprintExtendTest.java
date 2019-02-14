package com.sequenceiq.cloudbreak.clusterdefinition.kerberos;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.clusterdefinition.AmbariBlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.type.KerberosType;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@RunWith(MockitoJUnitRunner.class)
public class KerberosBlueprintExtendTest {

    private static final ImmutableMap<String, String> KERBEROS_ENV;

    static {
        KERBEROS_ENV = ImmutableMap.<String, String>builder()
                .put("realm", "NODE.DC1.CONSUL")
                .put("kdc_type", "mit-kdc")
                .put("kdc_hosts", "hostname.node.dc1.consul")
                .put("admin_server_host", "hostname1.node.dc1.consul")
                .put("encryption_types", "aes des3-cbc-sha1 rc4 des-cbc-md5")
                .put("ldap_url", "")
                .put("container_dn", "")
                .build();
    }

    @Spy
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    @InjectMocks
    @Spy
    private KerberosClusterDefinitionService underTest;

    @Test
    public void testAddKerberosConfigToClearBlueprint() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-kerb-forced.json");

        AmbariBlueprintTextProcessor b = new AmbariBlueprintTextProcessor(json);
        String result = underTest.extendBlueprintWithKerberos(b, KERBEROS_ENV, "node.dc1.consul,node.consul", true, null,
                KerberosType.MIT, false).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testAddKerberosConfigTokerverosConfiguredBlueprintNotForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-default-kerb.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-kerb.json");

        AmbariBlueprintTextProcessor b = new AmbariBlueprintTextProcessor(json);
        String result = underTest.extendBlueprintWithKerberos(b, KERBEROS_ENV, "node.dc1.consul,node.consul", true, null,
                KerberosType.MIT, false).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testAddKerberosConfigTokerverosConfiguredBlueprintForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-default-kerb.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-kerb-forced.json");

        AmbariBlueprintTextProcessor b = new AmbariBlueprintTextProcessor(json);
        String result = underTest.extendBlueprintWithKerberos(b, KERBEROS_ENV, "node.dc1.consul,node.consul", true, null,
                KerberosType.MIT, true).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testAddKerberosConfigToKerberosConfiguredBlueprintWithDescriptorNotForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-default-kerb_descriptor.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-default-kerb_descriptor_fixed.json");

        AmbariBlueprintTextProcessor b = new AmbariBlueprintTextProcessor(json);
        String result = underTest.extendBlueprintWithKerberos(b, KERBEROS_ENV, "node.dc1.consul,node.consul", false, null,
                KerberosType.MIT, false).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testAddKerberosConfigToKerberosConfiguredBlueprintWithDescriptorForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-default-kerb_descriptor.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-default-kerb_descriptor_forced_fixed.json");

        AmbariBlueprintTextProcessor b = new AmbariBlueprintTextProcessor(json);
        String result = underTest.extendBlueprintWithKerberos(b, KERBEROS_ENV, "node.dc1.consul,node.consul", false, null,
                KerberosType.MIT, true).asText();

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

}
