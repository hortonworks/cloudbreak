package com.sequenceiq.cloudbreak.clusterdefinition.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
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
import com.sequenceiq.cloudbreak.json.JsonHelper;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@RunWith(MockitoJUnitRunner.class)
public class AmbariBlueprintUtilsTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private JsonHelper jsonHelper;

    @InjectMocks
    private AmbariBlueprintUtils underTest;

    @Test
    public void testReadDefaultBlueprintFromFileWhenFileExistThenShouldReadFile() throws IOException {
        String blueprint = underTest.readDefaultBlueprintFromFile(new String[]{"hdp26-druid-bi"});
        Assert.assertNotNull(blueprint);
    }

    @Test
    public void testReadDefaultBlueprintFromFileWhenFileDoesNotExistNetShouldThrowException() throws IOException {
        thrown.expect(FileNotFoundException.class);
        thrown.expectMessage("class path resource [defaults/blueprints/hdp26-druid-bi1.bp] cannot be opened because it does not exist");

        String blueprint = underTest.readDefaultBlueprintFromFile(new String[]{"hdp26-druid-bi1"});
        Assert.assertNotNull(blueprint);
    }

    @Test
    public void testCountHostGroupsWhenBlueprintContainTwoHostgroupShouldReturnTwo() throws IOException {
        String blueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        JsonNode root = JsonUtil.readTree(blueprint);

        Assert.assertEquals(2L, underTest.countHostGroups(root));
    }

    @Test
    public void testGetBlueprintNameWhenNameIsPresentedInBlueprintThenShouldReturnWithThatName() throws IOException {
        String blueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        JsonNode root = JsonUtil.readTree(blueprint);

        Assert.assertEquals("multi-node-hdfs-yarn", underTest.getBlueprintName(root));
    }

    @Test
    public void testGetBlueprintHdpVersionWhenVersionIsPresentedInBlueprintThenShouldReturnWithThatVersion() throws IOException {
        String blueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        JsonNode root = JsonUtil.readTree(blueprint);

        Assert.assertEquals("2.5", underTest.getBlueprintStackVersion(root));
    }

    @Test
    public void testGetBlueprintStackTypeWhenVersionIsPresentedInBlueprintThenShouldReturnWithThatStackType() throws IOException {
        String blueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        JsonNode root = JsonUtil.readTree(blueprint);

        Assert.assertEquals("HDP", underTest.getBlueprintStackName(root));
    }

    @Test
    public void testConvertStringToJsonNodeWhenBluerprintIsValidInBlueprintThenShouldReturnWithThatBlueprintJson() throws IOException {
        String blueprint = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        JsonNode jsonFromString = JsonUtil.readTree(blueprint);

        when(jsonHelper.createJsonFromString(blueprint)).thenReturn(jsonFromString);
        JsonNode jsonNode = underTest.convertStringToJsonNode(blueprint);

        Assert.assertNotNull(jsonNode);
        verify(jsonHelper, times(1)).createJsonFromString(blueprint);
    }

    @Test
    public void testIsBlueprintNamePreConfiguredWhenTheConfigIncorrectThenShouldReturnFalse() {
        String blueprintStrings = "EDW-ETL: Apache Hive, Apache Spark 2=hdp26-etl-edw-spark2=test1";

        String[] split = blueprintStrings.split("=");
        assertFalse(underTest.isBlueprintNamePreConfigured(blueprintStrings, split));
    }

    @Test
    public void testIsBlueprintNamePreConfiguredWhenTheConfigCorrectAndTwoSegmentThenShouldReturnTrue() {
        String blueprintStrings = "EDW-ETL: Apache Hive, Apache Spark 2=hdp26-etl-edw-spark2";

        String[] split = blueprintStrings.split("=");
        assertTrue(underTest.isBlueprintNamePreConfigured(blueprintStrings, split));
    }

    @Test
    public void testIsBlueprintNamePreConfiguredWhenTheConfigCorrectAndOneSegmentThenShouldReturnTrue() {
        String blueprintStrings = "hdp26-etl-edw-spark2";

        String[] split = blueprintStrings.split("=");
        assertTrue(underTest.isBlueprintNamePreConfigured(blueprintStrings, split));
    }

    @Test
    public void testIsValidHostGroupNameWithNull() {
        assertFalse(underTest.isValidHostGroupName(null));
    }

    @Test
    public void testIsValidHostGroupNameWithEmpty() {
        assertFalse(underTest.isValidHostGroupName(""));
    }

    @Test
    public void testIsValidHostGroupNameWithUnderScores() {
        assertTrue(underTest.isValidHostGroupName("host_group_123"));
    }

    @Test
    public void testIsValidHostGroupNameWithDashes() {
        assertFalse(underTest.isValidHostGroupName("host-group-123"));
    }

    @Test
    public void testIsValidHostGroupNameWithMaster() {
        assertTrue(underTest.isValidHostGroupName("master"));
    }
}