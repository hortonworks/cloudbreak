package com.sequenceiq.datalake.service.rotation;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.service.multicluster.InterServiceMultiClusterRotationService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1RotationEndpoint;

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
                () -> distroXV1RotationEndpoint.checkOngoingChildrenMultiSecretRotations(parentResourceCrn, multiSecretType.value(), userCrn));
    }

    @Override
    public void markChildren(String parentResourceCrn, MultiSecretType multiSecretType) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        ThreadBasedUserCrnProvider.doAsInternalActor(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> distroXV1RotationEndpoint.markMultiClusterChildrenResources(parentResourceCrn, multiSecretType.value(), userCrn));
    }
}
