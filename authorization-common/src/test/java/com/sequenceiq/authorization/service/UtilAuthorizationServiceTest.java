package com.sequenceiq.authorization.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.authorization.AuthorizationProto;
import com.google.common.collect.Lists;
import com.sequenceiq.authorization.info.model.CheckResourceRightV4Response;
import com.sequenceiq.authorization.info.model.CheckResourceRightsV4Request;
import com.sequenceiq.authorization.info.model.CheckResourceRightsV4Response;
import com.sequenceiq.authorization.info.model.CheckRightOnResourcesV4Request;
import com.sequenceiq.authorization.info.model.CheckRightOnResourcesV4Response;
import com.sequenceiq.authorization.info.model.CheckRightV4Request;
import com.sequenceiq.authorization.info.model.CheckRightV4Response;
import com.sequenceiq.authorization.info.model.ResourceRightsV4;
import com.sequenceiq.authorization.info.model.RightV4;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.list.ResourceListProvider;
import com.sequenceiq.authorization.service.list.Resource;
import com.sequenceiq.authorization.service.list.ResourceFilteringService;
import com.sequenceiq.authorization.service.model.HasRightOnAny;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@ExtendWith(MockitoExtension.class)
public class UtilAuthorizationServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:1234:user:91011";

    @Mock
    private UmsRightProvider umsRightProvider;

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Mock
    private ResourceCrnAthorizationFactory resourceCrnAthorizationFactory;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ResourceListProvider authorizationResourceProvider;

    @Mock
    private ResourceFilteringService resourceFilteringService;

    @InjectMocks
    private UtilAuthorizationService underTest;

    @BeforeEach
    public void setup() {
        Stream.of(AuthorizationResourceAction.values()).forEach(action ->
                lenient().when(umsRightProvider.getRight(eq(action))).thenReturn(action.getRight()));
        ReflectionTestUtils.setField(underTest, "authorizationResourceProvider", Optional.of(authorizationResourceProvider));
    }

    @Test
    public void testCheckRight() {
        when(grpcUmsClient.hasRights(anyString(), any(), any())).thenReturn(Lists.newArrayList(Boolean.TRUE, Boolean.FALSE));

        CheckRightV4Request rightReq = new CheckRightV4Request();
        rightReq.setRights(Lists.newArrayList(RightV4.ENV_CREATE, RightV4.DISTROX_READ));
        CheckRightV4Response rightResult = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.checkRights(rightReq));

        rightResult.getResponses().forEach(checkRightV4SingleResponse -> {
            if (checkRightV4SingleResponse.getRight().equals(RightV4.ENV_CREATE)) {
                assertTrue(checkRightV4SingleResponse.getResult());
            }
            if (checkRightV4SingleResponse.getRight().equals(RightV4.DISTROX_READ)) {
                assertFalse(checkRightV4SingleResponse.getResult());
            }
        });

        verify(grpcUmsClient, times(1)).hasRights(anyString(), any(), any());
    }

    @Test
    public void testCheckResourceRightFallback() {
        when(grpcUmsClient.hasRights(anyString(), any(), any()))
                .thenReturn(Lists.newLinkedList(Arrays.asList(Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE)));

        CheckResourceRightsV4Request rightReq = new CheckResourceRightsV4Request();
        List<ResourceRightsV4> resourceRights = new LinkedList<>();
        resourceRights.add(createResourceRightV4("envCrn", RightV4.ENV_STOP, RightV4.ENV_START));
        resourceRights.add(createResourceRightV4("dhCrn", RightV4.DH_START, RightV4.DH_STOP));
        rightReq.setResourceRights(resourceRights);
        CheckResourceRightsV4Response rightResult = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.checkRightsOnResources(rightReq));

        rightResult.getResponses().forEach(checkResourceRightV4SingleResponse -> checkResourceRightV4SingleResponse.getRights()
                .forEach(checkRightV4SingleResponse -> {
                    if (checkRightV4SingleResponse.getRight().equals(RightV4.ENV_STOP) || checkRightV4SingleResponse.getRight().equals(RightV4.DH_STOP)) {
                        assertTrue(checkRightV4SingleResponse.getResult());
                    }
                    if (checkRightV4SingleResponse.getRight().equals(RightV4.DH_START) || checkRightV4SingleResponse.getRight().equals(RightV4.ENV_START)) {
                        assertFalse(checkRightV4SingleResponse.getResult());
                    }
                }));

        verify(grpcUmsClient, times(1)).hasRights(anyString(), any(), any());
    }

    @Test
    public void testCheckResourceRight() {
        AuthorizationProto.RightCheck dhStartRightCheck = AuthorizationProto.RightCheck.newBuilder()
                .setRight(RightV4.DH_START.getAction().getRight()).setResource("dhCrn").build();
        AuthorizationProto.RightCheck dhStartEnvRightCheck = AuthorizationProto.RightCheck.newBuilder()
                .setRight(RightV4.DH_START.getAction().getRight()).setResource("envCrn").build();
        AuthorizationProto.RightCheck dhStopRightCheck = AuthorizationProto.RightCheck.newBuilder()
                .setRight(RightV4.DH_STOP.getAction().getRight()).setResource("dhCrn").build();
        AuthorizationProto.RightCheck dhStopEnvRightCheck = AuthorizationProto.RightCheck.newBuilder()
                .setRight(RightV4.DH_STOP.getAction().getRight()).setResource("envCrn").build();
        AuthorizationProto.RightCheck dlRepairRightCheck = AuthorizationProto.RightCheck.newBuilder()
                .setRight(RightV4.SDX_REPAIR.getAction().getRight()).setResource("dlCrn").build();
        AuthorizationProto.RightCheck dlRepairEnvRightCheck = AuthorizationProto.RightCheck.newBuilder()
                .setRight(RightV4.SDX_REPAIR.getAction().getRight()).setResource("env2crn").build();
        AuthorizationProto.RightCheck dlUpgradeRightCheck = AuthorizationProto.RightCheck.newBuilder()
                .setRight(RightV4.SDX_UPGRADE.getAction().getRight()).setResource("dlCrn").build();
        AuthorizationProto.RightCheck dlUpgradeEnvRightCheck = AuthorizationProto.RightCheck.newBuilder()
                .setRight(RightV4.SDX_UPGRADE.getAction().getRight()).setResource("env2crn").build();
        AuthorizationProto.RightCheck dlRecoveryRightCheck = AuthorizationProto.RightCheck.newBuilder()
                .setRight(RightV4.SDX_RECOVER.getAction().getRight()).setResource("dlCrn").build();
        AuthorizationProto.RightCheck dlRecoveryEnvRightCheck = AuthorizationProto.RightCheck.newBuilder()
                .setRight(RightV4.SDX_RECOVER.getAction().getRight()).setResource("env2crn").build();
        when(grpcUmsClient.hasRights(anyString(), eq(Arrays.asList(dhStartRightCheck, dhStartEnvRightCheck, dhStopRightCheck,
                dhStopEnvRightCheck, dlRepairRightCheck, dlRepairEnvRightCheck, dlUpgradeRightCheck, dlUpgradeEnvRightCheck, dlRecoveryRightCheck,
                dlRecoveryEnvRightCheck)), any()))
                .thenReturn(Lists.newArrayList(Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, Boolean.TRUE,  Boolean.TRUE,
                        Boolean.FALSE, Boolean.TRUE, Boolean.FALSE));

        when(resourceCrnAthorizationFactory.calcAuthorization(eq("dhCrn"), eq(RightV4.DH_START.getAction())))
                .thenReturn(Optional.of(new HasRightOnAny(RightV4.DH_START.getAction(), Arrays.asList("dhCrn", "envCrn"))));
        when(resourceCrnAthorizationFactory.calcAuthorization(eq("dhCrn"), eq(RightV4.DH_STOP.getAction())))
                .thenReturn(Optional.of(new HasRightOnAny(RightV4.DH_STOP.getAction(), Arrays.asList("dhCrn", "envCrn"))));
        when(resourceCrnAthorizationFactory.calcAuthorization(eq("dlCrn"), eq(RightV4.SDX_REPAIR.getAction())))
                .thenReturn(Optional.of(new HasRightOnAny(RightV4.SDX_REPAIR.getAction(), Arrays.asList("dlCrn", "env2crn"))));
        when(resourceCrnAthorizationFactory.calcAuthorization(eq("dlCrn"), eq(RightV4.SDX_UPGRADE.getAction())))
                .thenReturn(Optional.of(new HasRightOnAny(RightV4.SDX_UPGRADE.getAction(), Arrays.asList("dlCrn", "env2crn"))));
        when(resourceCrnAthorizationFactory.calcAuthorization(eq("dlCrn"), eq(RightV4.SDX_RECOVER.getAction())))
                .thenReturn(Optional.of(new HasRightOnAny(RightV4.SDX_RECOVER.getAction(), Arrays.asList("dlCrn", "env2crn"))));

        CheckResourceRightsV4Request rightReq = new CheckResourceRightsV4Request();
        rightReq.setResourceRights(Lists.newArrayList(createResourceRightV4("dhCrn", RightV4.DH_START, RightV4.DH_STOP),

                createResourceRightV4("dlCrn", RightV4.SDX_REPAIR, RightV4.SDX_UPGRADE, RightV4.SDX_RECOVER)));
        CheckResourceRightsV4Response rightResult = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.checkRightsOnResources(rightReq));

        rightResult.getResponses().forEach(checkResourceRightV4SingleResponse -> checkResourceRightV4SingleResponse.getRights()
                .forEach(checkRightV4SingleResponse -> {
                    if (checkRightV4SingleResponse.getRight().equals(RightV4.DH_START)) {
                        assertFalse(checkRightV4SingleResponse.getResult());
                    }
                    if (checkRightV4SingleResponse.getRight().equals(RightV4.DH_STOP)) {
                        assertTrue(checkRightV4SingleResponse.getResult());
                    }
                    if (checkRightV4SingleResponse.getRight().equals(RightV4.SDX_REPAIR)) {
                        assertTrue(checkRightV4SingleResponse.getResult());
                    }
                    if (checkRightV4SingleResponse.getRight().equals(RightV4.SDX_UPGRADE)) {
                        assertTrue(checkRightV4SingleResponse.getResult());
                    }
                    if (checkRightV4SingleResponse.getRight().equals(RightV4.SDX_RECOVER)) {
                        assertTrue(checkRightV4SingleResponse.getResult());
                    }
                }));

        verify(grpcUmsClient, times(1)).hasRights(anyString(), any(), any());
    }

    @Test
    public void testHasRightsOnResourcesThrowsBadRequestWhenNotEntitled() {
        CheckRightOnResourcesV4Request request = new CheckRightOnResourcesV4Request();
        request.setRight(RightV4.DH_DESCRIBE);
        request.setResourceCrns(List.of(DATAHUB_CRN));
        when(entitlementService.listFilteringEnabled(eq("1234"))).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.checkRightOnResources(request)));

        assertEquals("Resource based filtering is not enabled in the current account.", exception.getMessage());
    }

    @Test
    public void testHasRightsOnResourcesThrowsUnsupportedOperationWhenNotImplementedOnResource() {
        ReflectionTestUtils.setField(underTest, "authorizationResourceProvider", Optional.empty());
        CheckRightOnResourcesV4Request request = new CheckRightOnResourcesV4Request();
        request.setRight(RightV4.DH_DESCRIBE);
        request.setResourceCrns(List.of(DATAHUB_CRN));
        when(entitlementService.listFilteringEnabled(eq("1234"))).thenReturn(true);

        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.checkRightOnResources(request)));

        assertEquals("Service doesn't support resource filtering.", exception.getMessage());
    }

    @Test
    public void testHasRightsOnResources() {
        CheckRightOnResourcesV4Request request = new CheckRightOnResourcesV4Request();
        request.setRight(RightV4.DH_DESCRIBE);
        request.setResourceCrns(List.of(DATAHUB_CRN));
        when(entitlementService.listFilteringEnabled(eq("1234"))).thenReturn(true);
        doAnswer(invocation -> ((List<String>) invocation.getArgument(1)).stream()
                .map(crn -> new Resource(crn, Optional.empty()))
                .collect(Collectors.toList())
        ).when(authorizationResourceProvider).findResources(anyString(), anyList());
        doAnswer(invocation -> ((Function<Predicate<String>, List<CheckResourceRightV4Response>>) invocation.getArgument(3))
                .apply(crn -> true)).when(resourceFilteringService).filter(any(), any(), anyList(), any());

        CheckRightOnResourcesV4Response response = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.checkRightOnResources(request));

        assertEquals(RightV4.DH_DESCRIBE, response.getRight());
        assertEquals(1, response.getResponses().size());
        CheckResourceRightV4Response checkResourceRightV4Response = response.getResponses().get(0);
        assertTrue(checkResourceRightV4Response.isResult());
        assertEquals(DATAHUB_CRN, checkResourceRightV4Response.getResourceCrn());
    }

    private ResourceRightsV4 createResourceRightV4(String crn, RightV4... rights) {
        ResourceRightsV4 resourceRightsV4 = new ResourceRightsV4();
        resourceRightsV4.setResourceCrn(crn);
        resourceRightsV4.setRights(Lists.newArrayList(rights));
        return resourceRightsV4;
    }
}
