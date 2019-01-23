package com.sequenceiq.cloudbreak.template.filesystem;

import java.util.Map;

import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.model.ExecutionType;

public class FileSystemScriptConfigTest {

    @Test
    public void testFileSystemScriptConfigWhenNoPropertiesPopulatedThenPropertiesShouldBeEmpty() {
        FileSystemScriptConfig fileSystemScriptConfig = new FileSystemScriptConfig("test", RecipeType.POST_CLUSTER_MANAGER_START, ExecutionType.ALL_NODES);
        Assert.assertEquals(fileSystemScriptConfig.getProperties(), Maps.newHashMap());
        Assert.assertEquals(fileSystemScriptConfig.getExecutionType(), ExecutionType.ALL_NODES);
        Assert.assertEquals(fileSystemScriptConfig.getRecipeType(), RecipeType.POST_CLUSTER_MANAGER_START);
        Assert.assertEquals(fileSystemScriptConfig.getScriptLocation(), "test");
    }

    @Test
    public void testFileSystemScriptConfigWhenPropertiesPopulatedThenPropertiesShouldNotBeEmpty() {
        Map<String, String> map = Maps.newHashMap();
        map.put("test1", "testvalue1");

        FileSystemScriptConfig fileSystemScriptConfig =
                new FileSystemScriptConfig("test", RecipeType.POST_CLUSTER_MANAGER_START, ExecutionType.ALL_NODES, map);
        Assert.assertEquals(fileSystemScriptConfig.getProperties(), map);
        Assert.assertEquals(fileSystemScriptConfig.getExecutionType(), ExecutionType.ALL_NODES);
        Assert.assertEquals(fileSystemScriptConfig.getRecipeType(), RecipeType.POST_CLUSTER_MANAGER_START);
        Assert.assertEquals(fileSystemScriptConfig.getScriptLocation(), "test");
    }

}