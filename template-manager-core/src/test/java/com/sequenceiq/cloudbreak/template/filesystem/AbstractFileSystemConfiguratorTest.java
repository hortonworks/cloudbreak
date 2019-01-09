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

import com.sequenceiq.cloudbreak.api.model.ExecutionType;
import com.sequenceiq.cloudbreak.api.model.RecipeType;
import com.sequenceiq.cloudbreak.template.filesystem.adls.AdlsFileSystemConfigurationsView;
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
        AdlsFileSystemConfigurationsView adlsFileSystemConfigurationsView = new AdlsFileSystemConfigurationsView(adlsFileSystem(), new HashSet<>(), false);
        List<RecipeScript> actual = underTest.getScripts(credential, adlsFileSystemConfigurationsView);

        List<RecipeScript> expected = singletonList(new RecipeScript("echo 'newContent'", ExecutionType.ALL_NODES, RecipeType.POST_AMBARI_START));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetScriptsWhenNoReplace() {
        Credential credential = new Credential();
        credential.setId(1L);
        AdlsFileSystemConfigurationsView adlsFileSystemConfigurationsView = new AdlsFileSystemConfigurationsView(adlsFileSystem(), new HashSet<>(), false);
        List<RecipeScript> actual = underTest.getScripts(credential, adlsFileSystemConfigurationsView);

        List<RecipeScript> expected = singletonList(new RecipeScript("echo '$replace'", ExecutionType.ALL_NODES, RecipeType.POST_AMBARI_START));
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetScriptsWhenFileNotFound() {
        Credential credential = new Credential();
        credential.setId(2L);
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
