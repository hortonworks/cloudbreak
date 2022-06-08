package com.sequenceiq.authorization.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.cloudera.thunderhead.service.authorization.AuthorizationProto.RightCheck;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.model.HasRight;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;

@ExtendWith(MockitoExtension.class)
public class ResourceAuthorizationServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @Mock
    private UmsRightProvider umsRightProvider;

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Spy
    private List<AuthorizationFactory<? extends Annotation>> authorizationFactories = new ArrayList<>();

    @InjectMocks
    private ResourceAuthorizationService underTest;

    @Mock
    private AuthorizationFactory<CheckPermissionByResourceCrn> authorizationFactory1;

    @Mock
    private AuthorizationFactory<CheckPermissionByResourceName> authorizationFactory2;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private ResourceNameFactoryService resourceNameFactoryService;

    @Captor
    private ArgumentCaptor<List<RightCheck>> captor;

    @BeforeEach
    public void setUp() {
        authorizationFactories.add(authorizationFactory1);
        authorizationFactories.add(authorizationFactory2);
        when(authorizationFactory1.supportedAnnotation()).thenReturn(CheckPermissionByResourceCrn.class);
        when(authorizationFactory2.supportedAnnotation()).thenReturn(CheckPermissionByResourceName.class);
        lenient().when(umsRightProvider.getRightMapper()).thenReturn(AuthorizationResourceAction::getRight);
    }

    @Test
    public void testAccessDenied() throws NoSuchMethodException {
        Method method = ExampleClass.class.getMethod("method", String.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(authorizationFactory1.getAuthorization(any(), any(), any(), any()))
                .thenReturn(Optional.of(new HasRight(AuthorizationResourceAction.EDIT_ENVIRONMENT, "crn")));
        when(grpcUmsClient.hasRights(anyString(), anyList(), any())).thenReturn(List.of(false));

        AccessDeniedException accessDeniedException = assertThrows(AccessDeniedException.class, () -> {
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.authorize(USER_CRN, proceedingJoinPoint, methodSignature));
        });

        assertEquals("Doesn't have 'environments/editEnvironment' right on unknown resource type [crn: crn].", accessDeniedException.getMessage());
    }

    @Test
    public void testSuccess() throws NoSuchMethodException {
        Method method = ExampleClass.class.getMethod("method", String.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(authorizationFactory1.getAuthorization(any(), any(), any(), any()))
                .thenReturn(Optional.of(new HasRight(AuthorizationResourceAction.EDIT_ENVIRONMENT, "crn")));
        when(grpcUmsClient.hasRights(anyString(), anyList(), any())).thenReturn(List.of(true));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.authorize(USER_CRN, proceedingJoinPoint, methodSignature));
    }

    @Test
    public void testAccessDeniedCombined() throws NoSuchMethodException {
        Method method = ExampleClass.class.getMethod("methodCombined", String.class, String.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(authorizationFactory1.getAuthorization(any(), any(), any(), any()))
                .thenReturn(Optional.of(new HasRight(AuthorizationResourceAction.EDIT_ENVIRONMENT, "crn1")));
        when(authorizationFactory2.getAuthorization(any(), any(), any(), any()))
                .thenReturn(Optional.of(new HasRight(AuthorizationResourceAction.DESCRIBE_CREDENTIAL, "crn2")));
        when(grpcUmsClient.hasRights(anyString(), anyList(), any())).thenReturn(List.of(false, false));

        AccessDeniedException accessDeniedException = assertThrows(AccessDeniedException.class, () -> {
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.authorize(USER_CRN, proceedingJoinPoint, methodSignature));
        });

        assertEquals("Not authorized for the following reasons. Doesn't have 'environments/editEnvironment' right on unknown resource type [crn: crn1]. " +
                "Doesn't have 'environments/describeCredential' right on unknown resource type [crn: crn2].", accessDeniedException.getMessage());

        verify(grpcUmsClient).hasRights(anyString(), captor.capture(), any());

        List<RightCheck> rightChecks = captor.getValue();
        assertEquals(2, rightChecks.size());
        assertEquals("environments/editEnvironment", rightChecks.get(0).getRight());
        assertEquals("crn1", rightChecks.get(0).getResource());
        assertEquals("environments/describeCredential", rightChecks.get(1).getRight());
        assertEquals("crn2", rightChecks.get(1).getResource());
    }

    private static class ExampleClass {

        @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
        public void method(@ResourceCrn String crn) {

        }

        @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
        @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_CREDENTIAL)
        public void methodCombined(@ResourceCrn String crn, @ResourceName String name) {

        }
    }
}