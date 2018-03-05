package com.sequenceiq.cloudbreak.blueprint.kerberos;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.blueprint.JacksonBlueprintProcessor;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@RunWith(MockitoJUnitRunner.class)
public class KerberosBlueprintExtendTest {

    @Spy
    private JacksonBlueprintProcessor blueprintProcessor;

    @InjectMocks
    @Spy
    private KerberosBlueprintService underTest;

    //@Spy
    //private AmbariClient underTest;

    @Test
    public void testAddKerberosConfigToClearBlueprint() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-kerb-forced.json");

        String result = underTest.extendBlueprintWithKerberos(json, "mit-kdc", "hostname.node.dc1.consul",
                "hostname1.node.dc1.consul", "NODE.DC1.CONSUL", "node.dc1.consul,node.consul",
                null, null, true, null, false);

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testAddKerberosConfigTokerverosConfiguredBlueprintNotForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-default-kerb.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-kerb.json");

        String result = underTest.extendBlueprintWithKerberos(json, "mit-kdc", "hostname.node.dc1.consul", "hostname1.node.dc1.consul",
                "NODE.DC1.CONSUL", "node.dc1.consul,node.consul", null, null, true, null, false);

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testAddKerberosConfigTokerverosConfiguredBlueprintForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-default-kerb.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-kerb-forced.json");

        String result = underTest.extendBlueprintWithKerberos(json, "mit-kdc", "hostname.node.dc1.consul", "hostname1.node.dc1.consul",
                "NODE.DC1.CONSUL", "node.dc1.consul,node.consul", null, null, true, null, true);

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testAddKerberosConfigToKerberosConfiguredBlueprintWithDescriptorNotForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-default-kerb_descriptor.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-default-kerb_descriptor_fixed.json");

        String result = underTest.extendBlueprintWithKerberos(json, "mit-kdc", "hostname.node.dc1.consul", "hostname1.node.dc1.consul",
                "NODE.DC1.CONSUL", "node.dc1.consul,node.consul", null, null, false, null, false);

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);
    }

    @Test
    public void testAddKerberosConfigToKerberosConfiguredBlueprintWithDescriptorForced() throws IOException {
        String json = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-default-kerb_descriptor.json");
        String expected = FileReaderUtils.readFileFromClasspath("extend-blueprint/multi-node-hdfs-yarn-default-kerb_descriptor_forced_fixed.json");

        String result = underTest.extendBlueprintWithKerberos(json, "mit-kdc", "hostname.node.dc1.consul", "hostname1.node.dc1.consul",
                "NODE.DC1.CONSUL", "node.dc1.consul,node.consul", null, null, false, null, true);

        JsonNode expectedNode = JsonUtil.readTree(expected);
        JsonNode resultNode = JsonUtil.readTree(result);
        Assert.assertEquals(expectedNode, resultNode);

    }

}
