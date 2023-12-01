package com.sequenceiq.distrox.v1.distrox.controller;

import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.service.stack.flow.StackRotationService;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXChildResourceMarkingRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXSecretRotationRequest;

@ExtendWith(MockitoExtension.class)
class DistroXV1RotationControllerTest {

    @Mock
    private StackRotationService stackRotationService;

    @InjectMocks
    private DistroXV1RotationController underTest;

    @Test
    public void testRotateSecrets() {
        DistroXSecretRotationRequest request = new DistroXSecretRotationRequest();
        String crn = "crn";
        request.setCrn(crn);
        List<String> secretTypes = List.of("SECRET_TYPE");
        request.setSecrets(secretTypes);
        request.setExecutionType(RotationFlowExecutionType.ROTATE);

        underTest.rotateSecrets(request);

        verify(stackRotationService).rotateSecrets(crn, secretTypes, RotationFlowExecutionType.ROTATE, null);
    }

    @Test
    public void testCheckOngoingChildrenMultiSecretRotationsByParent() {
        String crn = "crn";
        String secretType = "SECRET_TYPE";

        underTest.checkOngoingChildrenMultiSecretRotationsByParent(crn, secretType, "userCrn");

        verify(stackRotationService).checkOngoingChildrenMultiSecretRotations(crn, secretType);
    }

    @Test
    public void testMarkMultiClusterChildrenResourcesByParent() {
        DistroXChildResourceMarkingRequest request = new DistroXChildResourceMarkingRequest();
        String crn = "crn";
        request.setParentCrn(crn);
        String secretType = "SECRET_TYPE";
        request.setSecret(secretType);

        underTest.markMultiClusterChildrenResourcesByParent(request, "userCrn");

        verify(stackRotationService).markMultiClusterChildrenResources(crn, secretType);
    }
}