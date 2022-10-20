package com.sequenceiq.cloudbreak.template.filesystem;

import static com.sequenceiq.cloudbreak.template.filesystem.TemplateCoreTestUtil.adlsFileSystem;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.sequenceiq.cloudbreak.common.model.recipe.RecipeScript;
import com.sequenceiq.cloudbreak.common.model.recipe.RecipeType;
import com.sequenceiq.cloudbreak.common.type.ExecutionType;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.template.filesystem.adls.AdlsFileSystemConfigurationsView;

public class AbstractFileSystemConfiguratorTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private final AbstractFileSystemConfiguratorImpl underTest = new AbstractFileSystemConfiguratorImpl();

    @Test
    public void testGetScripts() {
        Credential credential = Credential.builder().crn("crn0").build();
        AdlsFileSystemConfigurationsView adlsFileSystemConfigurationsView = new AdlsFileSystemConfigurationsView(adlsFileSystem(), new HashSet<>(), false);
        List<RecipeScript> actual = underTest.getScripts(credential, adlsFileSystemConfigurationsView);

        List<RecipeScript> expected = singletonList(new RecipeScript("echo 'newContent'", ExecutionType.ALL_NODES, RecipeType.POST_CLOUDERA_MANAGER_START));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetScriptsWhenNoReplace() {
        Credential credential = Credential.builder().crn("crn1").build();
        AdlsFileSystemConfigurationsView adlsFileSystemConfigurationsView = new AdlsFileSystemConfigurationsView(adlsFileSystem(), new HashSet<>(), false);
        List<RecipeScript> actual = underTest.getScripts(credential, adlsFileSystemConfigurationsView);

        List<RecipeScript> expected = singletonList(new RecipeScript("echo '$replace'", ExecutionType.ALL_NODES, RecipeType.POST_CLOUDERA_MANAGER_START));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetScriptsWhenFileNotFound() {
        Credential credential = Credential.builder().crn("crn2").build();
        AdlsFileSystemConfigurationsView adlsFileSystemConfigurationsView = new AdlsFileSystemConfigurationsView(adlsFileSystem(), new HashSet<>(), false);

        thrown.expectMessage("Filesystem configuration scripts cannot be read.");
        thrown.expect(FileSystemConfigException.class);

        underTest.getScripts(credential, adlsFileSystemConfigurationsView);
    }

    @Test
    public void testCreateResource() {
        AdlsFileSystemConfigurationsView adlsFileSystemConfigurationsView = new AdlsFileSystemConfigurationsView(adlsFileSystem(), new HashSet<>(), false);

        Map<String, String> actual = underTest.createResources(adlsFileSystemConfigurationsView);

        Assert.assertEquals(emptyMap(), actual);
    }

    @Test
    public void testDelteResource() {
        AdlsFileSystemConfigurationsView adlsFileSystemConfigurationsView = new AdlsFileSystemConfigurationsView(adlsFileSystem(), new HashSet<>(), false);

        Map<String, String> actual = underTest.deleteResources(adlsFileSystemConfigurationsView);

        Assert.assertEquals(emptyMap(), actual);
    }
}
