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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.customdomain.CustomDomainSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.EnvironmentSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.StackInputs;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackV4RequestToStackConverter;
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
public class StackV4RequestToStackConverterTest {

    @InjectMocks
    private StackV4RequestToStackConverter underTest;

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
        StackAuthenticationV4Request stackAuthenticationRequest = mock(StackAuthenticationV4Request.class);
        StackAuthentication stackAuthentication = mock(StackAuthentication.class);
        Network network = mock(Network.class);
        NetworkV4Request networkV4Request = mock(NetworkV4Request.class);
        ClusterV4Request clusterV4Request = mock(ClusterV4Request.class);
        Cluster cluster = mock(Cluster.class);

        StackV4Request source = new StackV4Request();
        EnvironmentSettingsV4Request environmentSettingsV4Request = new EnvironmentSettingsV4Request();
        environmentSettingsV4Request.setName("environmentName");
        environmentSettingsV4Request.setCredentialName("credentialName");
        environmentSettingsV4Request.setName("name");
        PlacementSettingsV4Request placementSettings = new PlacementSettingsV4Request();
        placementSettings.setAvailabilityZone("availablityZone");
        placementSettings.setRegion("region");
        environmentSettingsV4Request.setPlacement(placementSettings);
        source.setEnvironment(environmentSettingsV4Request);
        CustomDomainSettingsV4Request customDomainSettings = new CustomDomainSettingsV4Request();
        customDomainSettings.setHostgroupNameAsHostname(true);
        customDomainSettings.setClusterNameAsSubdomain(true);
        customDomainSettings.setHostname("customHostname");
        customDomainSettings.setDomainName("customDomain");
        source.setCustomDomain(customDomainSettings);
        source.setAuthentication(stackAuthenticationRequest);
        source.setNetwork(networkV4Request);
        source.setCluster(clusterV4Request);
        ImageSettingsV4Request imageSettings = new ImageSettingsV4Request();
        imageSettings.setOs("os");
        imageSettings.setId("imageId");
        imageSettings.setCatalog("imageCatalog");
        source.setImage(imageSettings);

        when(credentialService.getByNameForWorkspace(source.getEnvironment().getCredentialName(), workspace)).thenReturn(credential);
        when(environmentViewService.getByNameForWorkspace(source.getEnvironment().getName(), workspace)).thenReturn(environment);
        when(conversionService.convert(source.getAuthentication(), StackAuthentication.class)).thenReturn(stackAuthentication);
        when(conversionService.convert(source.getNetwork(), Network.class)).thenReturn(network);
        when(conversionService.convert(source.getCluster(), Cluster.class)).thenReturn(cluster);

        Stack actual = underTest.convert(source);

        assertThat(actual.getName(), is(source.getName()));
        assertThat(actual.getDisplayName(), is(source.getName()));
        assertThat(actual.getCredential(), is(credential));
        assertThat(actual.getEnvironment(), is(environment));
        assertThat(actual.getAvailabilityZone(), is(source.getEnvironment().getPlacement().getAvailabilityZone()));
        assertThat(actual.getRegion(), is(source.getEnvironment().getPlacement().getRegion()));
        assertThat(actual.getStackAuthentication(), is(stackAuthentication));
        assertThat(actual.getNetwork(), is(network));
        assertThat(actual.getCustomDomain(), is(source.getCustomDomain().getDomainName()));
        assertThat(actual.getCustomHostname(), is(source.getCustomDomain().getHostname()));
        assertThat(actual.isClusterNameAsSubdomain(), is(source.getCustomDomain().isClusterNameAsSubdomain()));
        assertThat(actual.isHostgroupNameAsHostname(), is(source.getCustomDomain().isHostgroupNameAsHostname()));
        assertThat(actual.getCluster(), is(cluster));
        assertThat(actual.getInputs().get(StackInputs.class).getCustomInputs().size(), is(0));
        assertThat(actual.getTags(), is(nullValue()));
        assertThat(actual.getComponents().size(), is(1));

        Image image = actual.getComponents().iterator().next().getAttributes().get(Image.class);

        assertThat(image.getImageCatalogName(), is(source.getImage().getCatalog()));
        assertThat(image.getImageId(), is(source.getImage().getId()));
        assertThat(image.getOs(), is(source.getImage().getOs()));
    }

    @Test
    public void testConvertWhenNoGeneralAndPlacementAndNetworkAndCustomDomainAndClusterAndImage() throws IOException {
        StackV4Request source = new StackV4Request();

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
        ClusterV4Request clusterV4Request = mock(ClusterV4Request.class);
        Cluster cluster = new Cluster();
        InstanceGroupV4Request instanceGroupV4Request = mock(InstanceGroupV4Request.class);
        InstanceGroup instanceGroup = new InstanceGroup();
        HostGroup hostGroup = new HostGroup();

        StackV4Request source = new StackV4Request();
        source.setCluster(clusterV4Request);
        source.setInstanceGroups(Collections.singletonList(instanceGroupV4Request));

        when(conversionService.convert(source.getCluster(), Cluster.class)).thenReturn(cluster);
        when(conversionService.convert(instanceGroupV4Request, HostGroup.class)).thenReturn(hostGroup);
        when(conversionService.convert(instanceGroupV4Request, InstanceGroup.class)).thenReturn(instanceGroup);

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

        StackV4Request source = new StackV4Request();
        EnvironmentSettingsV4Request generalSettings = new EnvironmentSettingsV4Request();
        generalSettings.setName("environmentName");
        source.setEnvironment(generalSettings);

        when(environmentViewService.getByNameForWorkspace(source.getEnvironment().getName(), workspace)).thenReturn(environment);

        Stack actual = underTest.convert(source);

        assertThat(actual.cloudPlatform(), is(environment.getCloudPlatform()));

        Mockito.verify(environmentViewService, times(1)).getByNameForWorkspace(generalSettings.getName(), workspace);
        Mockito.verify(credentialService, times(0)).getByNameForWorkspace(any(), eq(workspace));
    }

    @Test
    public void testConvertWhenCloudPlatfromComesFromCredential() {
        Credential credential = new Credential();
        credential.setCloudPlatform("cloudPlatform");

        StackV4Request source = new StackV4Request();
        EnvironmentSettingsV4Request generalSettings = new EnvironmentSettingsV4Request();
        generalSettings.setCredentialName("credentialName");
        source.setEnvironment(generalSettings);

        when(credentialService.getByNameForWorkspace(source.getEnvironment().getCredentialName(), workspace)).thenReturn(credential);

        Stack actual = underTest.convert(source);

        assertThat(actual.cloudPlatform(), is(credential.cloudPlatform()));

        Mockito.verify(credentialService, times(1)).getByNameForWorkspace(generalSettings.getCredentialName(), workspace);
        Mockito.verify(environmentViewService, times(0)).getByNameForWorkspace(any(), eq(workspace));
    }
}
