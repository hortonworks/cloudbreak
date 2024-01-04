package com.sequenceiq.datalake.service.rotation;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.service.multicluster.InterServiceMultiClusterRotationService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1RotationEndpoint;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXChildResourceMarkingRequest;

@Service
public class SdxInterServiceMultiRotationService implements InterServiceMultiClusterRotationService {

    @Inject
    private DistroXV1RotationEndpoint distroXV1RotationEndpoint;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Override
    public boolean checkOngoingChildrenMultiSecretRotations(String parentResourceCrn, MultiSecretType multiSecretType) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return ThreadBasedUserCrnProvider.doAsInternalActor(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> distroXV1RotationEndpoint.checkOngoingChildrenMultiSecretRotationsByParent(parentResourceCrn, multiSecretType.value(), userCrn));
    }

    @Override
    public void markChildren(String parentResourceCrn, MultiSecretType multiSecretType) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        DistroXChildResourceMarkingRequest request = getRequest(parentResourceCrn, multiSecretType);
        ThreadBasedUserCrnProvider.doAsInternalActor(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> distroXV1RotationEndpoint.markMultiClusterChildrenResourcesByParent(request, userCrn));
    }

    private static DistroXChildResourceMarkingRequest getRequest(String parentResourceCrn, MultiSecretType multiSecretType) {
        DistroXChildResourceMarkingRequest request = new DistroXChildResourceMarkingRequest();
        request.setSecret(multiSecretType.value());
        request.setParentCrn(parentResourceCrn);
        return request;
    }
}
