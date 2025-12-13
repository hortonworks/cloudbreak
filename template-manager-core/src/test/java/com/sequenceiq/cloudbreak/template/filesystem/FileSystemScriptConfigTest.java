package com.sequenceiq.cloudbreak.template.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.common.type.ExecutionType;

class FileSystemScriptConfigTest {

    @Test
    void testFileSystemScriptConfigWhenNoPropertiesPopulatedThenPropertiesShouldBeEmpty() {
        FileSystemScriptConfig fileSystemScriptConfig = new FileSystemScriptConfig("test", RecipeType.POST_CLOUDERA_MANAGER_START, ExecutionType.ALL_NODES);
        assertEquals(fileSystemScriptConfig.getProperties(), Maps.newHashMap());
        assertEquals(fileSystemScriptConfig.getExecutionType(), ExecutionType.ALL_NODES);
        assertEquals(fileSystemScriptConfig.getRecipeType(), RecipeType.POST_CLOUDERA_MANAGER_START);
        assertEquals(fileSystemScriptConfig.getScriptLocation(), "test");
    }

    @Test
    void testFileSystemScriptConfigWhenPropertiesPopulatedThenPropertiesShouldNotBeEmpty() {
        Map<String, String> map = Maps.newHashMap();
        map.put("test1", "testvalue1");

        FileSystemScriptConfig fileSystemScriptConfig =
                new FileSystemScriptConfig("test", RecipeType.POST_CLOUDERA_MANAGER_START, ExecutionType.ALL_NODES, map);
        assertEquals(fileSystemScriptConfig.getProperties(), map);
        assertEquals(fileSystemScriptConfig.getExecutionType(), ExecutionType.ALL_NODES);
        assertEquals(fileSystemScriptConfig.getRecipeType(), RecipeType.POST_CLOUDERA_MANAGER_START);
        assertEquals(fileSystemScriptConfig.getScriptLocation(), "test");
    }

}