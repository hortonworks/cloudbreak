package com.sequenceiq.authorization.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.thunderhead.service.authorization.AuthorizationProto;
import com.google.common.collect.Lists;
import com.sequenceiq.authorization.info.model.CheckResourceRightsV4Request;
import com.sequenceiq.authorization.info.model.CheckResourceRightsV4Response;
import com.sequenceiq.authorization.info.model.CheckRightV4Request;
import com.sequenceiq.authorization.info.model.CheckRightV4Response;
import com.sequenceiq.authorization.info.model.ResourceRightsV4;
import com.sequenceiq.authorization.info.model.RightV4;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.model.HasRightOnAny;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;

@RunWith(MockitoJUnitRunner.class)
public class UtilAuthorizationServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    @Mock
    private UmsRightProvider umsRightProvider;

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Mock
    private ResourceCrnAthorizationFactory resourceCrnAthorizationFactory;

    @InjectMocks
    private UtilAuthorizationService underTest;

    @Before
    public void setup() {
        Stream.of(AuthorizationResourceAction.values()).forEach(action ->
                when(umsRightProvider.getRight(eq(action))).thenReturn(action.getRight()));
    }

    @Test
    public void testCheckRight() {
        when(grpcUmsClient.hasRights(anyString(), anyString(), any(), any())).thenReturn(Lists.newArrayList(Boolean.TRUE, Boolean.FALSE));

        CheckRightV4Request rightReq = new CheckRightV4Request();
        rightReq.setRights(Lists.newArrayList(RightV4.ENV_CREATE, RightV4.DISTROX_READ));
        CheckRightV4Response rightResult = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getRightResult(rightReq));

        rightResult.getResponses().forEach(checkRightV4SingleResponse -> {
            if (checkRightV4SingleResponse.getRight().equals(RightV4.ENV_CREATE)) {
                assertTrue(checkRightV4SingleResponse.getResult());
            }
            if (checkRightV4SingleResponse.getRight().equals(RightV4.DISTROX_READ)) {
                assertFalse(checkRightV4SingleResponse.getResult());
            }
        });

        verify(grpcUmsClient, times(1)).hasRights(anyString(), anyString(), any(), any());
    }

    @Test
    public void testCheckResourceRightFallback() {
        when(grpcUmsClient.hasRights(anyString(), anyString(), any(), any()))
                .thenReturn(Lists.newLinkedList(Arrays.asList(Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE)));

        CheckResourceRightsV4Request rightReq = new CheckResourceRightsV4Request();
        List<ResourceRightsV4> resourceRights = new LinkedList<>();
        resourceRights.add(createResourceRightV4("envCrn", RightV4.ENV_STOP, RightV4.ENV_START));
        resourceRights.add(createResourceRightV4("dhCrn", RightV4.DH_START, RightV4.DH_STOP));
        rightReq.setResourceRights(resourceRights);
        CheckResourceRightsV4Response rightResult = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getResourceRightsResult(rightReq));

        rightResult.getResponses().forEach(checkResourceRightV4SingleResponse -> checkResourceRightV4SingleResponse.getRights()
                .forEach(checkRightV4SingleResponse -> {
                    if (checkRightV4SingleResponse.getRight().equals(RightV4.ENV_STOP) || checkRightV4SingleResponse.getRight().equals(RightV4.DH_STOP)) {
                        assertTrue(checkRightV4SingleResponse.getResult());
                    }
                    if (checkRightV4SingleResponse.getRight().equals(RightV4.DH_START) || checkRightV4SingleResponse.getRight().equals(RightV4.ENV_START)) {
                        assertFalse(checkRightV4SingleResponse.getResult());
                    }
                }));

        verify(grpcUmsClient, times(1)).hasRights(anyString(), anyString(), any(), any());
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
        when(grpcUmsClient.hasRights(anyString(), anyString(), eq(Arrays.asList(dhStartRightCheck, dhStartEnvRightCheck, dhStopRightCheck,
                dhStopEnvRightCheck, dlRepairRightCheck, dlRepairEnvRightCheck, dlUpgradeRightCheck, dlUpgradeEnvRightCheck)), any()))
                .thenReturn(Lists.newArrayList(Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, Boolean.TRUE,  Boolean.TRUE,
                        Boolean.FALSE));

        when(resourceCrnAthorizationFactory.calcAuthorization(eq("dhCrn"), eq(RightV4.DH_START.getAction())))
                .thenReturn(Optional.of(new HasRightOnAny(RightV4.DH_START.getAction(), Arrays.asList("dhCrn", "envCrn"))));
        when(resourceCrnAthorizationFactory.calcAuthorization(eq("dhCrn"), eq(RightV4.DH_STOP.getAction())))
                .thenReturn(Optional.of(new HasRightOnAny(RightV4.DH_STOP.getAction(), Arrays.asList("dhCrn", "envCrn"))));
        when(resourceCrnAthorizationFactory.calcAuthorization(eq("dlCrn"), eq(RightV4.SDX_REPAIR.getAction())))
                .thenReturn(Optional.of(new HasRightOnAny(RightV4.SDX_REPAIR.getAction(), Arrays.asList("dlCrn", "env2crn"))));
        when(resourceCrnAthorizationFactory.calcAuthorization(eq("dlCrn"), eq(RightV4.SDX_UPGRADE.getAction())))
                .thenReturn(Optional.of(new HasRightOnAny(RightV4.SDX_UPGRADE.getAction(), Arrays.asList("dlCrn", "env2crn"))));

        CheckResourceRightsV4Request rightReq = new CheckResourceRightsV4Request();
        rightReq.setResourceRights(Lists.newArrayList(createResourceRightV4("dhCrn", RightV4.DH_START, RightV4.DH_STOP),
                createResourceRightV4("dlCrn", RightV4.SDX_REPAIR, RightV4.SDX_UPGRADE)));
        CheckResourceRightsV4Response rightResult = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getResourceRightsResult(rightReq));

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
                }));

        verify(grpcUmsClient, times(1)).hasRights(anyString(), anyString(), any(), any());
    }

    private ResourceRightsV4 createResourceRightV4(String crn, RightV4... rights) {
        ResourceRightsV4 resourceRightsV4 = new ResourceRightsV4();
        resourceRightsV4.setResourceCrn(crn);
        resourceRightsV4.setRights(Lists.newArrayList(rights));
        return resourceRightsV4;
    }
}
