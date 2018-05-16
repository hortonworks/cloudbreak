package com.sequenceiq.cloudbreak.blueprint.filesystem;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.sequenceiq.cloudbreak.api.model.ExecutionType;
import com.sequenceiq.cloudbreak.api.model.FileSystemConfiguration;
import com.sequenceiq.cloudbreak.api.model.RecipeType;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeScript;
import com.sequenceiq.cloudbreak.domain.Credential;

public class AbstractFileSystemConfiguratorTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private final AbstractFileSystemConfiguratorImpl underTest = new AbstractFileSystemConfiguratorImpl();

    @Test
    public void testGetScripts() {
        Credential credential = new Credential();
        credential.setId(0L);
        FileSystemConfiguration fsConfig = new FileSystemConfiguration();
        List<RecipeScript> actual = underTest.getScripts(credential, fsConfig);

        List<RecipeScript> expected = singletonList(new RecipeScript("echo 'newContent'", ExecutionType.ALL_NODES, RecipeType.POST_AMBARI_START));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetScriptsWhenNoReplace() {
        Credential credential = new Credential();
        credential.setId(1L);
        FileSystemConfiguration fsConfig = new FileSystemConfiguration();
        List<RecipeScript> actual = underTest.getScripts(credential, fsConfig);

        List<RecipeScript> expected = singletonList(new RecipeScript("echo '$replace'", ExecutionType.ALL_NODES, RecipeType.POST_AMBARI_START));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetScriptsWhenFileNotFound() {
        Credential credential = new Credential();
        credential.setId(2L);
        FileSystemConfiguration fsConfig = new FileSystemConfiguration();

        thrown.expectMessage("Filesystem configuration scripts cannot be read.");
        thrown.expect(FileSystemConfigException.class);

        underTest.getScripts(credential, fsConfig);
    }

    @Test
    public void testCreateResource() {
        FileSystemConfiguration fsConfig = new FileSystemConfiguration();
        Map<String, String> actual = underTest.createResources(fsConfig);

        Assert.assertEquals(emptyMap(), actual);
    }

    @Test
    public void testDelteResource() {
        FileSystemConfiguration fsConfig = new FileSystemConfiguration();
        Map<String, String> actual = underTest.deleteResources(fsConfig);

        Assert.assertEquals(emptyMap(), actual);
    }
}
