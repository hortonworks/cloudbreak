package com.sequenceiq.cloudbreak.clusterdefinition.hbase;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.clusterdefinition.AmbariBlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class HbaseConfigProviderTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    @Mock
    private AmbariBlueprintTextProcessor blueprintProcessor;

    @InjectMocks
    private HbaseConfigProvider underTest;

    @Test
    public void testCustomTextManipulationWhenThereAreMissingHbaseClients() throws IOException {
        String blueprintText = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        TemplatePreparationObject object = Builder.builder().build();
        Set<String> masters = Sets.newHashSet("master", "slave_1", "slave_2", "compute");
        Set<String> clients = Sets.newHashSet("slave_1", "slave_2", "compute_1");
        Set<String> missing = Sets.newHashSet("master", "compute");

        when(blueprintProcessor.getHostGroupsWithComponent("HBASE_MASTER")).thenReturn(masters);
        when(blueprintProcessor.getHostGroupsWithComponent("HBASE_CLIENT")).thenReturn(clients);
        when(blueprintProcessor.addComponentToHostgroups("HBASE_CLIENT", missing)).thenReturn(blueprintProcessor);
        when(blueprintProcessor.asText()).thenReturn(blueprintText);

        String result = underTest.customTextManipulation(object, blueprintProcessor).asText();

        Assert.assertEquals(blueprintText, result);
        verify(blueprintProcessor, times(1)).getHostGroupsWithComponent("HBASE_MASTER");
        verify(blueprintProcessor, times(1)).getHostGroupsWithComponent("HBASE_CLIENT");
        verify(blueprintProcessor, times(1)).addComponentToHostgroups("HBASE_CLIENT", missing);
    }

    @Test
    public void testCustomTextManipulationWhenThereAreNoMissingHbaseClients() throws IOException {
        String blueprintText = FileReaderUtils.readFileFromClasspath("blueprints-jackson/bp-kerberized-test.bp");

        TemplatePreparationObject object = Builder.builder().build();
        Set<String> masters = Sets.newHashSet("master", "slave_1", "slave_2", "compute");
        Set<String> clients = Sets.newHashSet("master", "slave_1", "slave_2", "compute");
        Set<String> missing = Sets.newHashSet();

        when(blueprintProcessor.getHostGroupsWithComponent("HBASE_MASTER")).thenReturn(masters);
        when(blueprintProcessor.getHostGroupsWithComponent("HBASE_CLIENT")).thenReturn(clients);
        when(blueprintProcessor.asText()).thenReturn(blueprintText);

        String result = underTest.customTextManipulation(object, blueprintProcessor).asText();

        Assert.assertEquals(blueprintText, result);
        verify(blueprintProcessor, times(1)).getHostGroupsWithComponent("HBASE_MASTER");
        verify(blueprintProcessor, times(1)).getHostGroupsWithComponent("HBASE_CLIENT");
        verify(blueprintProcessor, times(0)).addComponentToHostgroups("HBASE_CLIENT", missing);
    }

    @Test
    public void testComponentsWhenThereCallingShouldReturnHbaseMaster() {
        Set<String> masters = Sets.newHashSet("HBASE_MASTER");

        Set<String> components = underTest.components();

        Assert.assertEquals(masters, components);
    }

}