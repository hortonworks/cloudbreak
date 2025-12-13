package com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.CheckResult;
import com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.json.JsonHelper;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
class MaintenanceModeValidationServiceTest {

    private static final Long WORKSPACE_ID = 1L;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private StackImageUpdateService imageUpdateService;

    @Mock
    private JsonHelper jsonHelper;

    @InjectMocks
    private MaintenanceModeValidationService underTest;

    private Stack stack;

    private Cluster cluster;

    private StatedImage statedImage;

    private Image image;

    private String stackRepo;

    private Map<String, String> packageVersions = Collections.singletonMap("package", "version");

    private List<Warning> warnings;

    @BeforeEach
    void setup() {
        cluster = new Cluster();
        cluster.setId(1L);
        stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.AVAILABLE));
        stack.setId(1L);
        stack.setName("stackname");
        stack.setRegion("region");
        stack.setCloudPlatform("AWS");
        stack.setCluster(cluster);
        cluster.setStack(stack);

        Workspace workspace = mock(Workspace.class);
        lenient().when(workspace.getId()).thenReturn(WORKSPACE_ID);
        stack.setWorkspace(workspace);

        image = Image.builder()
                .withOs("centos7")
                .withUuid("uuid")
                .withImageSetsByProvider(Collections.singletonMap("AWS", Collections.emptyMap()))
                .withPackageVersions(packageVersions)
                .withAdvertised(true)
                .build();

        statedImage = StatedImage.statedImage(image, "url", "name");
        warnings = new ArrayList<Warning>();
        //CHECKSTYLE:OFF
        stackRepo = "{\n" +
                "  \"href\" : \"http://127.0.0.1/api/v1/stacks/HDP/versions/2.6/operating_systems/redhat7/repositories/HDP-2.6\",\n" +
                "  \"Repositories\" : {\n" +
                "    \"applicable_services\" : [ ],\n" +
                "    \"base_url\" : \"http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.5.0\",\n" +
                "    \"components\" : null,\n" +
                "    \"default_base_url\" : \"http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.5.0\",\n" +
                "    \"distribution\" : null,\n" +
                "    \"mirrors_list\" : null,\n" +
                "    \"os_type\" : \"redhat7\",\n" +
                "    \"repo_id\" : \"HDP-2.6\",\n" +
                "    \"repo_name\" : \"HDP\",\n" +
                "    \"stack_name\" : \"HDP\",\n" +
                "    \"stack_version\" : \"2.6\",\n" +
                "    \"tags\" : [ ],\n" +
                "    \"unique\" : false\n" +
                "  }\n" +
                "}";
        //CHECKSTYLE:ON
    }

    @Test
    void testValidateStackRepositoryOk() throws IOException {
        StackRepoDetails repoDetails = new StackRepoDetails();
        repoDetails.setHdpVersion("2.6.5.0");
        Map stackMap = new HashMap<>();
        stackMap.put(StackRepoDetails.REPO_ID_TAG, "HDP-2.6");
        stackMap.put(StackRepoDetails.REPOSITORY_VERSION, "2.6.5.0-292");
        stackMap.put(StackRepoDetails.CUSTOM_VDF_REPO_KEY,
                "http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.5.0/HDP-2.6.5.0-292.xml");
        stackMap.put("redhat7", "http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.5.0");
        repoDetails.setStack(stackMap);

        when(clusterComponentConfigProvider.getStackRepoDetails(eq(cluster.getId()))).thenReturn(repoDetails);
        when(jsonHelper.createJsonFromString(eq(stackRepo))).thenReturn(JsonUtil.readTree(stackRepo));
        warnings.addAll(underTest.validateStackRepository(cluster.getId(), stackRepo));
        assertEquals(0, warnings.size());
    }

    @Test
    void testValidateStackRepositoryNok() throws IOException {
        StackRepoDetails repoDetails = new StackRepoDetails();
        repoDetails.setHdpVersion("2.6.5.2");
        Map stackMap = new HashMap<>();
        stackMap.put(StackRepoDetails.REPO_ID_TAG, "HDP-2.5");
        stackMap.put(StackRepoDetails.REPOSITORY_VERSION, "2.6.5.1-292");
        stackMap.put(StackRepoDetails.CUSTOM_VDF_REPO_KEY,
                "http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.5.1/HDP-2.6.5.1-292.xml");
        stackMap.put("redhat7", "http://public-repo-1.hortonworks.com/HDP/centos7/2.x/updates/2.6.5.1");
        repoDetails.setStack(stackMap);

        when(clusterComponentConfigProvider.getStackRepoDetails(eq(cluster.getId()))).thenReturn(repoDetails);
        when(jsonHelper.createJsonFromString(eq(stackRepo))).thenReturn(JsonUtil.readTree(stackRepo));
        warnings.addAll(underTest.validateStackRepository(cluster.getId(), stackRepo));
        assertEquals(2, warnings.size());
    }

    @Test
    void testValidateImageCatalogOk() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent =
                new com.sequenceiq.cloudbreak.cloud.model.Image(
                        "imageName",
                        Collections.emptyMap(),
                        image.getOs(),
                        image.getOsType(),
                        image.getArchitecture(),
                        statedImage.getImageCatalogUrl(),
                        statedImage.getImageCatalogName(),
                        "uuid",
                        packageVersions,
                        null,
                        null,
                        null);
        when(componentConfigProviderService.getImage(anyLong())).thenReturn(imageInComponent);
        when(imageCatalogService.getImage(anyLong(), anyString(), anyString(), anyString())).thenReturn(statedImage);
        when(imageUpdateService.checkPackageVersions(any(Stack.class), any(StatedImage.class))).thenReturn(CheckResult.ok());
        warnings.addAll(underTest.validateImageCatalog(stack));
        assertEquals(0, warnings.size());
    }

    @Test
    void testValidateImageCatalogNok() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent =
                new com.sequenceiq.cloudbreak.cloud.model.Image(
                        "imageName",
                        Collections.emptyMap(),
                        image.getOs(),
                        image.getOsType(),
                        image.getArchitecture(),
                        statedImage.getImageCatalogUrl(),
                        statedImage.getImageCatalogName(),
                        "uuid",
                        packageVersions,
                        null,
                        null,
                        null);
        when(componentConfigProviderService.getImage(anyLong())).thenReturn(imageInComponent);
        when(imageCatalogService.getImage(anyLong(), anyString(), anyString(), anyString())).thenReturn(statedImage);
        when(imageUpdateService.checkPackageVersions(any(Stack.class), any(StatedImage.class))).thenReturn(CheckResult.failed("Failure"));
        warnings.addAll(underTest.validateImageCatalog(stack));
        assertEquals(1, warnings.size());
    }
}