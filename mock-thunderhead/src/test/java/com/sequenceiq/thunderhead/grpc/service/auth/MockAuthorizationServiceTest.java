package com.sequenceiq.thunderhead.grpc.service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.authorization.AuthorizationProto;
import com.cloudera.thunderhead.service.authorization.AuthorizationProto.CheckRightRequest;
import com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsRequest;
import com.cloudera.thunderhead.service.authorization.AuthorizationProto.HasRightsResponse;
import com.cloudera.thunderhead.service.authorization.AuthorizationProto.RightCheck;
import com.sequenceiq.thunderhead.grpc.service.auth.roles.MockEnvironmentUserResourceRole;

import io.grpc.stub.StreamObserver;

@ExtendWith(MockitoExtension.class)
class MockAuthorizationServiceTest {

    @InjectMocks
    private MockAuthorizationService underTest;

    @Spy
    private MockEnvironmentUserResourceRole mockEnvironmentUserResourceRole;

    @Mock
    private StreamObserver<AuthorizationProto.CheckRightResponse> checkRightResponseObserver;

    @Mock
    private StreamObserver<HasRightsResponse> hasRightsResponseObserver;

    @Test
    void checkRightAnyUser() {
        CheckRightRequest request = createCheckRightRequest("crn:cdp:iam:us-west-1:cloudera:user:any", "environments/accessEnvironment");

        underTest.checkRight(request, checkRightResponseObserver);
        verify(checkRightResponseObserver, times(1)).onCompleted();
    }

    @Test
    void checkRightEnvUserApproved() {
        CheckRightRequest request = createCheckRightRequest("crn:cdp:iam:us-west-1:cloudera:user:envuser0", "environments/accessEnvironment");

        underTest.checkRight(request, checkRightResponseObserver);
        verify(checkRightResponseObserver, times(1)).onCompleted();
    }

    @Test
    void checkRightEnvUserDenied() {
        CheckRightRequest request = createCheckRightRequest("crn:cdp:iam:us-west-1:cloudera:user:envuser0", "environments/write");

        underTest.checkRight(request, checkRightResponseObserver);
        verify(checkRightResponseObserver, times(1)).onError(any());
    }

    @Test
    void hasRightsAnyUserAnyRight() {
        HasRightsRequest request = createHasRightsRequest("crn:cdp:iam:us-west-1:cloudera:user:any", "foo", "bar");
        underTest.hasRights(request, hasRightsResponseObserver);

        // Capture the HasRightsResponse passed to onNext
        ArgumentCaptor<HasRightsResponse> responseCaptor = ArgumentCaptor.forClass(HasRightsResponse.class);
        verify(hasRightsResponseObserver).onNext(responseCaptor.capture());

        // Verify that onCompleted was called exactly once
        verify(hasRightsResponseObserver, times(1)).onCompleted();

        // Retrieve the captured response
        HasRightsResponse capturedResponse = responseCaptor.getValue();

        // Assert that each result in the response is true
        capturedResponse.getResultList().forEach(result ->
                assertTrue(result, "Expected true for all rights checks but found false.")
        );
    }

    @Test
    void hasRightsEnvUserAnyRight() {
        HasRightsRequest request = createHasRightsRequest("crn:cdp:iam:us-west-1:cloudera:user:envuser0",
                "environments/accessEnvironment", "environments/write");
        underTest.hasRights(request, hasRightsResponseObserver);

        // Capture the HasRightsResponse passed to onNext
        ArgumentCaptor<HasRightsResponse> responseCaptor = ArgumentCaptor.forClass(HasRightsResponse.class);
        verify(hasRightsResponseObserver).onNext(responseCaptor.capture());

        // Verify that onCompleted was called exactly once
        verify(hasRightsResponseObserver, times(1)).onCompleted();

        // Retrieve the captured response
        HasRightsResponse capturedResponse = responseCaptor.getValue();


        // Assert conditions for the first two results
        List<Boolean> results = capturedResponse.getResultList();
        assertTrue(results.get(0), "Expected the first right check to be true but found it false.");
        assertFalse(results.get(1), "Expected the second right check to be false but found it true.");
        assertEquals(2, results.size(), "Expected exactly two results but found " + results.size());
    }

    private CheckRightRequest createCheckRightRequest(String actorCrn, String right) {
        RightCheck.Builder rightCheckBuilder = RightCheck.newBuilder().setRight(right);
        return CheckRightRequest.newBuilder()
                .setActorCrn(actorCrn)
                .setCheck(rightCheckBuilder.build())
                .build();
    }

    private HasRightsRequest createHasRightsRequest(String actorCrn, String... rights) {
        HasRightsRequest.Builder builder = HasRightsRequest.newBuilder()
                .setActorCrn(actorCrn);
        for (String right : rights) {
            RightCheck.Builder rightCheckBuilder = RightCheck.newBuilder().setRight(right);
            builder.addCheck(rightCheckBuilder.build());
        }
        return builder.build();
    }
}