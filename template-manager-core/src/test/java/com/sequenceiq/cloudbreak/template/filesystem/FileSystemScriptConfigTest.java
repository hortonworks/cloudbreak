package com.sequenceiq.cloudbreak.template.filesystem;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.model.ExecutionType;
import com.sequenceiq.cloudbreak.api.model.RecipeType;

public class FileSystemScriptConfigTest {

    @Test
    public void testFileSystemScriptConfigWhenNoPropertiesPopulatedThenPropertiesShouldBeEmpty() {
        FileSystemScriptConfig fileSystemScriptConfig = new FileSystemScriptConfig("test", RecipeType.POST_AMBARI_START, ExecutionType.ALL_NODES);
        Assert.assertEquals(fileSystemScriptConfig.getProperties(), Maps.newHashMap());
        Assert.assertEquals(fileSystemScriptConfig.getExecutionType(), ExecutionType.ALL_NODES);
        Assert.assertEquals(fileSystemScriptConfig.getRecipeType(), RecipeType.POST_AMBARI_START);
        Assert.assertEquals(fileSystemScriptConfig.getScriptLocation(), "test");
    }

    @Test
    public void testFileSystemScriptConfigWhenPropertiesPopulatedThenPropertiesShouldNotBeEmpty() {
        Map<String, String> map = Maps.newHashMap();
        map.put("test1", "testvalue1");

        FileSystemScriptConfig fileSystemScriptConfig =
                new FileSystemScriptConfig("test", RecipeType.POST_AMBARI_START, ExecutionType.ALL_NODES, map);
        Assert.assertEquals(fileSystemScriptConfig.getProperties(), map);
        Assert.assertEquals(fileSystemScriptConfig.getExecutionType(), ExecutionType.ALL_NODES);
        Assert.assertEquals(fileSystemScriptConfig.getRecipeType(), RecipeType.POST_AMBARI_START);
        Assert.assertEquals(fileSystemScriptConfig.getScriptLocation(), "test");
    }

}