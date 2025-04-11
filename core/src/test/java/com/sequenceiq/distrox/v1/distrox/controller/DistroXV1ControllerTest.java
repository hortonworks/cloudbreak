package com.sequenceiq.distrox.v1.distrox.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATAHUB;
import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ChangeImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackAddVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.rotaterdscert.StackRotateRdsCertificateV4Response;
import com.sequenceiq.cloudbreak.api.model.RotateRdsCertResponseType;
import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.rotaterdscert.StackRotateRdsCertificateService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackOperationService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXGenerateImageCatalogV1Response;
import com.sequenceiq.distrox.api.v1.distrox.model.rotaterdscert.RotateRdsCertificateV1Response;
import com.sequenceiq.distrox.v1.distrox.StackOperations;
import com.sequenceiq.distrox.v1.distrox.authorization.DataHubFiltering;
import com.sequenceiq.distrox.v1.distrox.converter.RotateRdsCertificateConverter;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class DistroXV1ControllerTest {

    private static final String CRN = "crn";

    private static final String NAME = "name";

    private static final String ACCOUNT_ID = "accountId";

    private static final String IMAGE_CATALOG = "image catalog";

    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:mockuser@cloudera.com";

    private static final Long WORKSPACE_ID = 1L;

    private static final FlowIdentifier FLOW_IDENTIFIER = new FlowIdentifier(FlowType.FLOW, "flowId");

    private static final RotateRdsCertificateV1Response RESPONSE =
            new RotateRdsCertificateV1Response(RotateRdsCertResponseType.TRIGGERED, FLOW_IDENTIFIER, null, CRN);

    @Mock
    private DataHubFiltering datahubFiltering;

    @Mock
    private StackOperations stackOperations;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private Workspace workspace;

    @Mock
    private StackOperationService stackOperationService;

    @Mock
    private RotateRdsCertificateConverter rotateRdsCertificateConverter;

    @Mock
    private StackRotateRdsCertificateService stackRotateRdsCertificateService;

    @Captor
    private ArgumentCaptor<NameOrCrn> nameOrCrnArgumentCaptor;

    @InjectMocks
    private DistroXV1Controller distroXV1Controller;

    @Test
    void testListUsesListAuthorizationService() {
        StackViewV4Responses expected = new StackViewV4Responses();
        when(datahubFiltering.filterDataHubs(any(), anyString(), anyString())).thenReturn(expected);

        StackViewV4Responses actual = distroXV1Controller.list(NAME, CRN);

        assertEquals(expected, actual);
        verify(datahubFiltering).filterDataHubs(DESCRIBE_DATAHUB, NAME, CRN);
    }

    @Test
    void testChangeImageCatalog() {
        ChangeImageCatalogV4Request request = new ChangeImageCatalogV4Request();
        request.setImageCatalog(IMAGE_CATALOG);
        when(workspaceService.getForCurrentUser()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(WORKSPACE_ID);

        distroXV1Controller.changeImageCatalog(NAME, request);

        verify(stackOperations).changeImageCatalog(nameOrCrnArgumentCaptor.capture(), eq(WORKSPACE_ID), eq(IMAGE_CATALOG));
        assertEquals(NAME, nameOrCrnArgumentCaptor.getValue().getName());
    }

    @Test
    void testGenerateImageCatalog() {
        CloudbreakImageCatalogV3 imageCatalog = mock(CloudbreakImageCatalogV3.class);

        when(workspaceService.getForCurrentUser()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(WORKSPACE_ID);
        when(stackOperations.generateImageCatalog(nameOrCrnArgumentCaptor.capture(), eq(WORKSPACE_ID))).thenReturn(imageCatalog);

        DistroXGenerateImageCatalogV1Response actual = distroXV1Controller.generateImageCatalog(NAME);

        assertEquals(NAME, nameOrCrnArgumentCaptor.getValue().getName());
        assertEquals(imageCatalog, actual.getImageCatalog());
    }

    @Test
    void testRenewInternalCertificate() {
        FlowIdentifier flowIdentifier = mock(FlowIdentifier.class);
        Stack stack = mock(Stack.class);

        when(stackOperations.getStackByCrn(anyString())).thenReturn(stack);
        when(stackOperationService.renewCertificate(anyLong())).thenReturn(flowIdentifier);

        FlowIdentifier actual = distroXV1Controller.renewCertificate(CRN);

        assertEquals(actual, flowIdentifier);
    }

    @Test
    void testRotateSaltPasswordByCrn() {
        doAs(TEST_USER_CRN, () -> distroXV1Controller.rotateSaltPasswordByCrn(CRN));

        verify(stackOperations).rotateSaltPassword(NameOrCrn.ofCrn(CRN), ACCOUNT_ID, RotateSaltPasswordReason.MANUAL);
    }

    @Test
    void testUpdateSaltByCrn() {
        doAs(TEST_USER_CRN, () -> distroXV1Controller.updateSaltByCrn(CRN));

        verify(stackOperations).updateSalt(NameOrCrn.ofCrn(CRN), ACCOUNT_ID);
    }

    @Test
    void testModifyProxyInternal() {
        String previousProxyConfigCrn = "prev-proxy-crn";

        doAs(TEST_USER_CRN, () -> distroXV1Controller.modifyProxyInternal(CRN, previousProxyConfigCrn, "user-crn"));

        verify(stackOperationService).modifyProxyConfig(NameOrCrn.ofCrn(CRN), ACCOUNT_ID, previousProxyConfigCrn);
    }

    @Test
    void testDeleteVolumesByStackName() {
        StackDeleteVolumesRequest stackDeleteVolumesRequest = new StackDeleteVolumesRequest();
        stackDeleteVolumesRequest.setStackId(1L);
        stackDeleteVolumesRequest.setGroup("COMPUTE");

        doAs(TEST_USER_CRN, () -> distroXV1Controller.deleteVolumesByStackName(CRN, stackDeleteVolumesRequest));

        verify(stackOperations).putDeleteVolumes(NameOrCrn.ofName(CRN), ACCOUNT_ID, stackDeleteVolumesRequest);
    }

    @Test
    void testDeleteVolumesByStackCrn() {
        StackDeleteVolumesRequest stackDeleteVolumesRequest = new StackDeleteVolumesRequest();
        stackDeleteVolumesRequest.setStackId(1L);
        stackDeleteVolumesRequest.setGroup("COMPUTE");

        doAs(TEST_USER_CRN, () -> distroXV1Controller.deleteVolumesByStackCrn(CRN, stackDeleteVolumesRequest));

        verify(stackOperations).putDeleteVolumes(NameOrCrn.ofCrn(CRN), ACCOUNT_ID, stackDeleteVolumesRequest);
    }

    @Test
    void syncByNameTest() {
        doAs(TEST_USER_CRN, () -> distroXV1Controller.syncByName(NAME));

        verify(stackOperations).sync(NameOrCrn.ofName(NAME), ACCOUNT_ID, EnumSet.of(StackType.WORKLOAD));
    }

    @Test
    void syncByCrnTest() {
        doAs(TEST_USER_CRN, () -> distroXV1Controller.syncByCrn(CRN));

        verify(stackOperations).sync(NameOrCrn.ofCrn(CRN), ACCOUNT_ID, EnumSet.of(StackType.WORKLOAD));
    }

    @Test
    void testDiskUpdateByName() {
        DiskUpdateRequest diskUpdateRequest = mock(DiskUpdateRequest.class);
        doAs(TEST_USER_CRN, () -> {
            distroXV1Controller.diskUpdateByName(TEST_USER_CRN, diskUpdateRequest);
        });

        verify(stackOperationService).stackUpdateDisks(NameOrCrn.ofName(TEST_USER_CRN), diskUpdateRequest, ACCOUNT_ID);
    }

    @Test
    void testDiskUpdateByCrn() {
        DiskUpdateRequest diskUpdateRequest = mock(DiskUpdateRequest.class);
        doAs(TEST_USER_CRN, () -> {
            distroXV1Controller.diskUpdateByCrn(TEST_USER_CRN, diskUpdateRequest);
        });

        verify(stackOperationService).stackUpdateDisks(NameOrCrn.ofCrn(TEST_USER_CRN), diskUpdateRequest, ACCOUNT_ID);
    }

    @Test
    void testAddVolumesByStackName() {
        StackAddVolumesRequest stackAddVolumesRequest = new StackAddVolumesRequest();
        stackAddVolumesRequest.setInstanceGroup("COMPUTE");
        stackAddVolumesRequest.setCloudVolumeUsageType("GENERAL");
        stackAddVolumesRequest.setSize(200L);
        stackAddVolumesRequest.setType("gp2");
        stackAddVolumesRequest.setNumberOfDisks(2L);
        doAs(TEST_USER_CRN, () -> {
            distroXV1Controller.addVolumesByStackName(NAME, stackAddVolumesRequest);
        });

        verify(stackOperations).putAddVolumes(NameOrCrn.ofName(NAME), stackAddVolumesRequest, "accountId");
    }

    @Test
    void testAddVolumesByStackCrn() {
        StackAddVolumesRequest stackAddVolumesRequest = new StackAddVolumesRequest();
        stackAddVolumesRequest.setInstanceGroup("COMPUTE");
        stackAddVolumesRequest.setCloudVolumeUsageType("GENERAL");
        stackAddVolumesRequest.setSize(200L);
        stackAddVolumesRequest.setType("gp2");
        stackAddVolumesRequest.setNumberOfDisks(2L);
        doAs(TEST_USER_CRN, () -> {
            distroXV1Controller.addVolumesByStackCrn(CRN, stackAddVolumesRequest);
        });
        verify(stackOperations).putAddVolumes(NameOrCrn.ofCrn(CRN), stackAddVolumesRequest, "accountId");
    }

    @Test
    void testRotateRdsCertificateByName() {
        when(rotateRdsCertificateConverter.convert(any())).thenReturn(RESPONSE);
        when(stackRotateRdsCertificateService.rotateRdsCertificate(any(), anyString()))
                .thenReturn(new StackRotateRdsCertificateV4Response());

        RotateRdsCertificateV1Response result = doAs(TEST_USER_CRN, () -> distroXV1Controller.rotateRdsCertificateByName(NAME));
        verify(stackRotateRdsCertificateService).rotateRdsCertificate(any(), anyString());
        assertThat(result).isEqualTo(RESPONSE);
    }

    @Test
    void testRotateRdsCertificateByCrn() {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(CRN);
        when(rotateRdsCertificateConverter.convert(any())).thenReturn(RESPONSE);
        when(stackRotateRdsCertificateService.rotateRdsCertificate(nameOrCrn, ACCOUNT_ID))
                .thenReturn(new StackRotateRdsCertificateV4Response());

        RotateRdsCertificateV1Response result = doAs(TEST_USER_CRN, () -> distroXV1Controller.rotateRdsCertificateByCrn(CRN));
        verify(stackRotateRdsCertificateService).rotateRdsCertificate(nameOrCrn, ACCOUNT_ID);
        assertThat(result).isEqualTo(RESPONSE);
    }

    @Test
    void testUpdateRootVolumeByStackName() throws Exception {
        DiskUpdateRequest diskUpdateRequest = mock(DiskUpdateRequest.class);
        doAs(TEST_USER_CRN, () -> {
            try {
                distroXV1Controller.updateRootVolumeByDatahubName(NAME, diskUpdateRequest);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        verify(stackOperationService).rootVolumeDiskUpdate(NameOrCrn.ofName(NAME), diskUpdateRequest, "accountId");
    }

    @Test
    void testUpdateRootVolumeByStackCrn() throws Exception {
        DiskUpdateRequest diskUpdateRequest = mock(DiskUpdateRequest.class);
        doAs(TEST_USER_CRN, () -> {
            try {
                distroXV1Controller.updateRootVolumeByDatahubCrn(CRN, diskUpdateRequest);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        verify(stackOperationService).rootVolumeDiskUpdate(NameOrCrn.ofCrn(CRN), diskUpdateRequest, "accountId");
    }

    @Test
    void testListByServiceTypes() {
        List<String> serviceTypes = List.of("HIVE", "RANGER");
        Set<StackViewV4Response> stackViewV4ResponseSet = Set.of();
        Set<StackViewV4Response> result = Set.of();
        when(workspaceService.getForCurrentUser()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(WORKSPACE_ID);
        StackViewV4Responses stackViewV4Responses = mock(StackViewV4Responses.class);
        when(stackViewV4Responses.getResponses()).thenReturn(stackViewV4ResponseSet);
        when(datahubFiltering.filterResources(Crn.safeFromString(TEST_USER_CRN), DESCRIBE_DATAHUB, Map.of())).thenReturn(stackViewV4Responses);
        when(stackOperations.filterByServiceTypesPresent(WORKSPACE_ID, stackViewV4ResponseSet, serviceTypes)).thenReturn(result);

        StackViewV4Responses response = doAs(TEST_USER_CRN, () -> distroXV1Controller.listByServiceTypes(serviceTypes));

        assertEquals(result, response.getResponses());
    }

    @Test
    void testEnableSeLinuxByName() {
        doAs(TEST_USER_CRN, () -> {
            try {
                distroXV1Controller.modifySeLinuxByName(NAME, SeLinux.ENFORCING);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        verify(stackOperationService).triggerEnableSELinux(NameOrCrn.ofName(NAME), "accountId");
    }

    @Test
    void testEnableSeLinuxByCrn() {
        doAs(TEST_USER_CRN, () -> {
            try {
                distroXV1Controller.modifySeLinuxByCrn(CRN, SeLinux.ENFORCING);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        verify(stackOperationService).triggerEnableSELinux(NameOrCrn.ofCrn(CRN), "accountId");
    }
}
