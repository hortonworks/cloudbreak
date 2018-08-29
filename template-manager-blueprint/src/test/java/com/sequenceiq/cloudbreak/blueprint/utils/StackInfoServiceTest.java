package com.sequenceiq.cloudbreak.blueprint.utils;

import static org.mockito.Matchers.any;
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
import com.sequenceiq.cloudbreak.template.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class StackInfoServiceTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private BlueprintUtils blueprintUtils;

    @InjectMocks
    private StackInfoService stackInfoService;

    @Test
    public void blueprintStackInfoWhenWeCanDetectTheStackInfoAndStackVersion() throws IOException {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        when(blueprintUtils.getBlueprintStackVersion(any(JsonNode.class))).thenReturn("2.6");
        when(blueprintUtils.getBlueprintStackName(any(JsonNode.class))).thenReturn("HDP");

        stackInfoService.blueprintStackInfo(testBlueprint);

        verify(blueprintUtils, times(1)).getBlueprintStackVersion(any(JsonNode.class));
        verify(blueprintUtils, times(1)).getBlueprintStackName(any(JsonNode.class));
    }

    @Test
    public void blueprintStackInfoWhenReadTreeThrowsException() {
        String testBlueprint = "not-a-valid-bluepint";

        thrown.expect(BlueprintProcessingException.class);
        thrown.expectMessage("Unable to detect BlueprintStackInfo from the source blueprint which was: not-a-valid-bluepint.");

        stackInfoService.blueprintStackInfo(testBlueprint);

        verify(blueprintUtils, times(0)).getBlueprintStackVersion(any(JsonNode.class));
        verify(blueprintUtils, times(0)).getBlueprintStackName(any(JsonNode.class));
    }

    @Test
    public void hdfClusterWhenWeCanDetectTheStackInfoAndStackVersionAndTypeIsHDFThenShouldReturnTrue() throws IOException {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        when(blueprintUtils.getBlueprintStackVersion(any(JsonNode.class))).thenReturn("2.6");
        when(blueprintUtils.getBlueprintStackName(any(JsonNode.class))).thenReturn("HDF");

        Assert.assertTrue(stackInfoService.isHdfCluster(testBlueprint));

        verify(blueprintUtils, times(1)).getBlueprintStackVersion(any(JsonNode.class));
        verify(blueprintUtils, times(1)).getBlueprintStackName(any(JsonNode.class));
    }

    @Test
    public void hdfClusterWhenWeCanDetectTheStackInfoAndStackVersionAndTypeIsHDPThenShouldReturnFalse() throws IOException {
        String testBlueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        when(blueprintUtils.getBlueprintStackVersion(any(JsonNode.class))).thenReturn("2.6");
        when(blueprintUtils.getBlueprintStackName(any(JsonNode.class))).thenReturn("HDP");

        Assert.assertFalse(stackInfoService.isHdfCluster(testBlueprint));

        verify(blueprintUtils, times(1)).getBlueprintStackVersion(any(JsonNode.class));
        verify(blueprintUtils, times(1)).getBlueprintStackName(any(JsonNode.class));
    }

    @Test
    public void hdfClusterWhenReadTreeThrowsException() {
        String testBlueprint = "not-a-valid-bluepint";

        Assert.assertFalse(stackInfoService.isHdfCluster(testBlueprint));

        verify(blueprintUtils, times(0)).getBlueprintStackVersion(any(JsonNode.class));
        verify(blueprintUtils, times(0)).getBlueprintStackName(any(JsonNode.class));
    }
}