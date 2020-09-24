package com.sequenceiq.cloudbreak.service.authorization;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.UmsRightProvider;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.base.ResourceRightsV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.base.RightV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.CheckResourceRightsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.CheckRightV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.CheckResourceRightsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.CheckRightV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;

@RunWith(MockitoJUnitRunner.class)
public class UtilAuthorizationServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    @Mock
    private UmsRightProvider umsRightProvider;

    @Mock
    private GrpcUmsClient grpcUmsClient;

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

        rightResult.getResponses().stream().forEach(checkRightV4SingleResponse -> {
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
    public void testCheckResourceRight() {
        when(grpcUmsClient.hasRights(anyString(), anyString(), any(), any()))
                .thenReturn(Lists.newArrayList(Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE));

        CheckResourceRightsV4Request rightReq = new CheckResourceRightsV4Request();
        rightReq.setResourceRights(Lists.newArrayList(createResourceRightV4("envCrn", RightV4.ENV_STOP, RightV4.ENV_START),
                createResourceRightV4("dhCrn", RightV4.DH_START, RightV4.DH_STOP)));
        CheckResourceRightsV4Response rightResult = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getResourceRightsResult(rightReq));

        rightResult.getResponses().stream().forEach(checkResourceRightV4SingleResponse -> checkResourceRightV4SingleResponse.getRights().stream()
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

    private ResourceRightsV4 createResourceRightV4(String crn, RightV4... rights) {
        ResourceRightsV4 resourceRightsV4 = new ResourceRightsV4();
        resourceRightsV4.setResourceCrn(crn);
        resourceRightsV4.setRights(Lists.newArrayList(rights));
        return resourceRightsV4;
    }
}
