package com.sequenceiq.cloudbreak.blueprint.hadoop;

import static java.util.Collections.emptyMap;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject.Builder;
import com.sequenceiq.cloudbreak.blueprint.ConfigService;
import com.sequenceiq.cloudbreak.blueprint.HdfClusterLocator;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;

@RunWith(MockitoJUnitRunner.class)
public class HadoopConfigurationServiceTest {

    @InjectMocks
    private final HadoopConfigurationService underTest = new HadoopConfigurationService();

    @Mock
    private ConfigService configService;

    @Mock
    private HdfClusterLocator hdfClusterLocator;

    @Test
    public void testConfigure() throws IOException {
        AmbariClient ambariClient = mock(AmbariClient.class);

        BlueprintPreparationObject source = Builder.builder()
                .withAmbariClient(ambariClient)
                .build();
        String blueprintText = "Default";
        String bpTextWithHG = blueprintText + ", extended by HG";
        String bpTextWithG = bpTextWithHG + ", extended by G";

        when(configService.getHostGroupConfiguration(blueprintText, source.getHostGroups())).thenReturn(emptyMap());
        when(ambariClient.extendBlueprintHostGroupConfiguration(eq(blueprintText), anyMap())).thenReturn(bpTextWithHG);
        when(configService.getComponentsByHostGroup(bpTextWithHG, source.getHostGroups())).thenReturn(emptyMap());
        when(ambariClient.extendBlueprintGlobalConfiguration(eq(bpTextWithHG), anyMap())).thenReturn(bpTextWithG);

        String actual = underTest.configure(source, blueprintText);

        Assert.assertEquals("Default, extended by HG, extended by G", actual);
    }

    @Test
    public void testAdditionalCriteriaWhenTrue() {
        StackRepoDetails stackRepoDetails = new StackRepoDetails();
        BlueprintPreparationObject source = Builder.builder().withStackRepoDetails(stackRepoDetails).build();

        when(hdfClusterLocator.hdfCluster(stackRepoDetails)).thenReturn(false);

        boolean actual = underTest.additionalCriteria(source, "blueprintText");
        Assert.assertTrue(actual);
    }

    @Test
    public void testAdditionalCriteriaWhenFalse() {
        StackRepoDetails stackRepoDetails = new StackRepoDetails();
        BlueprintPreparationObject source = Builder.builder().withStackRepoDetails(stackRepoDetails).build();

        when(hdfClusterLocator.hdfCluster(stackRepoDetails)).thenReturn(true);

        boolean actual = underTest.additionalCriteria(source, "blueprintText");
        Assert.assertFalse(actual);
    }
}
