package com.sequenceiq.cloudbreak.converter.v2;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.model.stack.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.CustomDomainSettings;
import com.sequenceiq.cloudbreak.api.model.v2.GeneralSettings;
import com.sequenceiq.cloudbreak.api.model.v2.ImageSettings;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.PlacementSettings;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.StackInputs;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentViewService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@RunWith(MockitoJUnitRunner.class)
public class StackV2RequestToStackConverterTest {

    @InjectMocks
    private StackV2RequestToStackConverter underTest;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private CloudParameterService cloudParameterService;

    @Mock
    private UserService userService;

    @Mock
    private CredentialService credentialService;

    @Mock
    private EnvironmentViewService environmentViewService;

    @Mock
    private ConversionService conversionService;

    private Workspace workspace;

    @Before
    public void before() {

        workspace = new Workspace();
        workspace.setId(100L);
        workspace.setName("TEST_WS_NAME");
        workspace.setDescription("TEST_WS_DESC");

        CloudbreakUser cloudbreakUser = mock(CloudbreakUser.class);
        User user = mock(User.class);

        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        when(userService.getOrCreate(cloudbreakUser)).thenReturn(user);
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(workspace.getId());
        when(workspaceService.get(workspace.getId(), user)).thenReturn(workspace);
    }

    @Test
    public void testConvert() throws IOException {
        Credential credential = mock(Credential.class);
        EnvironmentView environment = mock(EnvironmentView.class);
        StackAuthenticationRequest stackAuthenticationRequest = mock(StackAuthenticationRequest.class);
        StackAuthentication stackAuthentication = mock(StackAuthentication.class);
        Network network = mock(Network.class);
        NetworkV2Request networkV2Request = mock(NetworkV2Request.class);
        ClusterV2Request clusterV2Request = mock(ClusterV2Request.class);
        Cluster cluster = mock(Cluster.class);

        StackV2Request source = new StackV2Request();
        GeneralSettings generalSettings = new GeneralSettings();
        generalSettings.setEnvironmentName("environmentName");
        generalSettings.setCredentialName("credentialName");
        generalSettings.setName("name");
        source.setGeneral(generalSettings);
        PlacementSettings placementSettings = new PlacementSettings();
        placementSettings.setAvailabilityZone("availablityZone");
        placementSettings.setRegion("region");
        source.setPlacement(placementSettings);
        source.setPlatformVariant("platformVariant");
        CustomDomainSettings customDomainSettings = new CustomDomainSettings();
        customDomainSettings.setHostgroupNameAsHostname(true);
        customDomainSettings.setClusterNameAsSubdomain(true);
        customDomainSettings.setCustomHostname("customHostname");
        customDomainSettings.setCustomDomain("customDomain");
        source.setCustomDomain(customDomainSettings);
        source.setStackAuthentication(stackAuthenticationRequest);
        source.setNetwork(networkV2Request);
        source.setCluster(clusterV2Request);
        ImageSettings imageSettings = new ImageSettings();
        imageSettings.setOs("os");
        imageSettings.setImageId("imageId");
        imageSettings.setImageCatalog("imageCatalog");
        source.setImageSettings(imageSettings);

        when(credentialService.getByNameForWorkspace(source.getGeneral().getCredentialName(), workspace)).thenReturn(credential);
        when(environmentViewService.getByNameForWorkspace(source.getGeneral().getEnvironmentName(), workspace)).thenReturn(environment);
        when(conversionService.convert(source.getStackAuthentication(), StackAuthentication.class)).thenReturn(stackAuthentication);
        when(conversionService.convert(source.getNetwork(), Network.class)).thenReturn(network);
        when(conversionService.convert(source.getCluster(), Cluster.class)).thenReturn(cluster);

        Stack actual = underTest.convert(source);

        assertThat(actual.getName(), is(source.getGeneral().getName()));
        assertThat(actual.getDisplayName(), is(source.getGeneral().getName()));
        assertThat(actual.getCredential(), is(credential));
        assertThat(actual.getEnvironment(), is(environment));
        assertThat(actual.getAvailabilityZone(), is(source.getPlacement().getAvailabilityZone()));
        assertThat(actual.getRegion(), is(source.getPlacement().getRegion()));
        assertThat(actual.getStackAuthentication(), is(stackAuthentication));
        assertThat(actual.getPlatformVariant(), is(source.getPlatformVariant()));
        assertThat(actual.getNetwork(), is(network));
        assertThat(actual.getCustomDomain(), is(source.getCustomDomain().getCustomDomain()));
        assertThat(actual.getCustomHostname(), is(source.getCustomDomain().getCustomHostname()));
        assertThat(actual.isClusterNameAsSubdomain(), is(source.getCustomDomain().isClusterNameAsSubdomain()));
        assertThat(actual.isHostgroupNameAsHostname(), is(source.getCustomDomain().isHostgroupNameAsHostname()));
        assertThat(actual.getCluster(), is(cluster));
        assertThat(actual.getInputs().get(StackInputs.class).getCustomInputs().size(), is(0));
        assertThat(actual.getTags(), is(nullValue()));
        assertThat(actual.getComponents().size(), is(1));

        Image image = actual.getComponents().iterator().next().getAttributes().get(Image.class);

        assertThat(image.getImageCatalogName(), is(source.getImageSettings().getImageCatalog()));
        assertThat(image.getImageId(), is(source.getImageSettings().getImageId()));
        assertThat(image.getOs(), is(source.getImageSettings().getOs()));
    }

    @Test
    public void testConvertWhenNoGeneralAndPlacementAndNetworkAndCustomDomainAndClusterAndImage() throws IOException {
        StackV2Request source = new StackV2Request();

        Stack actual = underTest.convert(source);

        assertThat(actual.getName(), isEmptyOrNullString());
        assertThat(actual.getDisplayName(), isEmptyOrNullString());
        assertThat(actual.getCredential(), is(nullValue()));
        assertThat(actual.getEnvironment(), is(nullValue()));
        assertThat(actual.getAvailabilityZone(), isEmptyOrNullString());
        assertThat(actual.getRegion(), isEmptyOrNullString());
        assertThat(actual.getStackAuthentication(), is(nullValue()));
        assertThat(actual.getNetwork(), is(nullValue()));
        assertThat(actual.getCustomDomain(), isEmptyOrNullString());
        assertThat(actual.getCustomHostname(), isEmptyOrNullString());
        assertThat(actual.getCluster(), is(nullValue()));
        assertThat(actual.getComponents().size(), is(0));

        Mockito.verify(credentialService, times(0)).getByNameForWorkspace(anyString(), any());
        Mockito.verify(environmentViewService, times(0)).getByNameForWorkspace(anyString(), any());
        Mockito.verify(conversionService, times(0)).convert(any(), eq(StackAuthentication.class));
        Mockito.verify(conversionService, times(0)).convert(any(), eq(Network.class));
        Mockito.verify(conversionService, times(0)).convert(any(), eq(Cluster.class));
    }

    @Test
    public void testConvertWhenHasClusterAndInstanceGroup() {
        ClusterV2Request clusterV2Request = mock(ClusterV2Request.class);
        Cluster cluster = new Cluster();
        InstanceGroupV2Request instanceGroupV2Request = mock(InstanceGroupV2Request.class);
        InstanceGroup instanceGroup = new InstanceGroup();
        HostGroup hostGroup = new HostGroup();

        StackV2Request source = new StackV2Request();
        source.setCluster(clusterV2Request);
        source.setInstanceGroups(Collections.singletonList(instanceGroupV2Request));

        when(conversionService.convert(source.getCluster(), Cluster.class)).thenReturn(cluster);
        when(conversionService.convert(instanceGroupV2Request, HostGroup.class)).thenReturn(hostGroup);
        when(conversionService.convert(instanceGroupV2Request, InstanceGroup.class)).thenReturn(instanceGroup);

        Stack actual = underTest.convert(source);

        assertThat(actual.getCluster(), is(cluster));
        assertThat(cluster.getHostGroups().size(), is(1));
        assertThat(cluster.getHostGroups().iterator().next(), is(hostGroup));
        assertThat(cluster.getHostGroups().size(), is(1));
        InstanceGroup next = actual.getInstanceGroups().iterator().next();
        assertThat(next, is(instanceGroup));
        assertThat(next.getStack(), is(actual));
        assertThat(hostGroup.getCluster(), is(cluster));

        Mockito.verify(conversionService, times(1)).convert(any(), eq(HostGroup.class));
        Mockito.verify(conversionService, times(1)).convert(any(), eq(InstanceGroup.class));
    }

    @Test
    public void testConvertWhenCloudPlatfromComesFromEnvironemnt() {
        EnvironmentView environment = new EnvironmentView();
        environment.setCloudPlatform("cloudPlatform");

        StackV2Request source = new StackV2Request();
        GeneralSettings generalSettings = new GeneralSettings();
        generalSettings.setEnvironmentName("environmentName");
        source.setGeneral(generalSettings);

        when(environmentViewService.getByNameForWorkspace(source.getGeneral().getEnvironmentName(), workspace)).thenReturn(environment);

        Stack actual = underTest.convert(source);

        assertThat(actual.cloudPlatform(), is(environment.getCloudPlatform()));

        Mockito.verify(environmentViewService, times(1)).getByNameForWorkspace(generalSettings.getEnvironmentName(), workspace);
        Mockito.verify(credentialService, times(0)).getByNameForWorkspace(any(), eq(workspace));
    }

    @Test
    public void testConvertWhenCloudPlatfromComesFromCredential() {
        Credential credential = new Credential();
        credential.setCloudPlatform("cloudPlatform");

        StackV2Request source = new StackV2Request();
        GeneralSettings generalSettings = new GeneralSettings();
        generalSettings.setCredentialName("credentialName");
        source.setGeneral(generalSettings);

        when(credentialService.getByNameForWorkspace(source.getGeneral().getCredentialName(), workspace)).thenReturn(credential);

        Stack actual = underTest.convert(source);

        assertThat(actual.cloudPlatform(), is(credential.cloudPlatform()));

        Mockito.verify(credentialService, times(1)).getByNameForWorkspace(generalSettings.getCredentialName(), workspace);
        Mockito.verify(environmentViewService, times(0)).getByNameForWorkspace(any(), eq(workspace));
    }

    @Test
    public void testConvertWhenCloudPlatfromComesFromPlatfomVariant() {
        String cloudPlatform = "cloudPlatform";

        StackV2Request source = new StackV2Request();
        source.setPlatformVariant("platformVariant");

        when(cloudParameterService.getPlatformByVariant(source.getPlatformVariant())).thenReturn("cloudPlatform");

        Stack actual = underTest.convert(source);

        assertThat(actual.cloudPlatform(), is(cloudPlatform));

        Mockito.verify(credentialService, times(0)).getByNameForWorkspace(any(), eq(workspace));
        Mockito.verify(environmentViewService, times(0)).getByNameForWorkspace(any(), eq(workspace));
    }
}
