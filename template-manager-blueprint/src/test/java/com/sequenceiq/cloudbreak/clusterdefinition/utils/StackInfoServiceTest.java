package com.sequenceiq.cloudbreak.clusterdefinition.utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.template.ClusterDefinitionProcessingException;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class StackInfoServiceTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private AmbariBlueprintUtils ambariBlueprintUtils;

    @InjectMocks
    private StackInfoService stackInfoService;

    @Test
    public void blueprintStackInfoWhenWeCanDetectTheStackInfoAndStackVersion() throws IOException {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        when(ambariBlueprintUtils.getBlueprintStackVersion(any(JsonNode.class))).thenReturn("2.6");
        when(ambariBlueprintUtils.getBlueprintStackName(any(JsonNode.class))).thenReturn("HDP");

        stackInfoService.clusterDefinitionStackInfo(testBlueprint);

        verify(ambariBlueprintUtils, times(1)).getBlueprintStackVersion(any(JsonNode.class));
        verify(ambariBlueprintUtils, times(1)).getBlueprintStackName(any(JsonNode.class));
    }

    @Test
    public void blueprintStackInfoWhenReadTreeThrowsException() {
        String testBlueprint = "not-a-valid-bluepint";

        thrown.expect(ClusterDefinitionProcessingException.class);
        thrown.expectMessage("Unable to detect ClusterDefinitionStackInfo from the source cluster definition which was: not-a-valid-bluepint.");

        stackInfoService.clusterDefinitionStackInfo(testBlueprint);

        verify(ambariBlueprintUtils, times(0)).getBlueprintStackVersion(any(JsonNode.class));
        verify(ambariBlueprintUtils, times(0)).getBlueprintStackName(any(JsonNode.class));
    }

    @Test
    public void hdfClusterWhenWeCanDetectTheStackInfoAndStackVersionAndTypeIsHDFThenShouldReturnTrue() throws IOException {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        when(ambariBlueprintUtils.getBlueprintStackVersion(any(JsonNode.class))).thenReturn("2.6");
        when(ambariBlueprintUtils.getBlueprintStackName(any(JsonNode.class))).thenReturn("HDF");

        Assert.assertTrue(stackInfoService.isHdfCluster(testBlueprint));

        verify(ambariBlueprintUtils, times(1)).getBlueprintStackVersion(any(JsonNode.class));
        verify(ambariBlueprintUtils, times(1)).getBlueprintStackName(any(JsonNode.class));
    }

    @Test
    public void hdfClusterWhenWeCanDetectTheStackInfoAndStackVersionAndTypeIsHDPThenShouldReturnFalse() throws IOException {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        when(ambariBlueprintUtils.getBlueprintStackVersion(any(JsonNode.class))).thenReturn("2.6");
        when(ambariBlueprintUtils.getBlueprintStackName(any(JsonNode.class))).thenReturn("HDP");

        Assert.assertFalse(stackInfoService.isHdfCluster(testBlueprint));

        verify(ambariBlueprintUtils, times(1)).getBlueprintStackVersion(any(JsonNode.class));
        verify(ambariBlueprintUtils, times(1)).getBlueprintStackName(any(JsonNode.class));
    }

    @Test
    public void hdfClusterWhenReadTreeThrowsException() {
        String testBlueprint = "not-a-valid-bluepint";

        Assert.assertFalse(stackInfoService.isHdfCluster(testBlueprint));

        verify(ambariBlueprintUtils, times(0)).getBlueprintStackVersion(any(JsonNode.class));
        verify(ambariBlueprintUtils, times(0)).getBlueprintStackName(any(JsonNode.class));
    }
}