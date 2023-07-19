package com.sequenceiq.freeipa.service.rotation;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.service.multicluster.InterServiceMultiClusterRotationService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1RotationEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxRotationEndpoint;

@Service
public class FreeipaInterServiceMultiRotationService implements InterServiceMultiClusterRotationService {

    @Inject
    private DistroXV1RotationEndpoint distroXV1RotationEndpoint;

    @Inject
    private SdxRotationEndpoint sdxRotationEndpoint;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Override
    public boolean checkOngoingChildrenMultiSecretRotations(String parentResourceCrn, MultiSecretType multiSecretType) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        Boolean distroxRotationOngoing = false;
        Boolean sdxRotationOngoing = false;
        if (multiSecretType.childSecretTypesByDescriptor().containsKey(CrnResourceDescriptor.DATAHUB)) {
            distroxRotationOngoing = ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> distroXV1RotationEndpoint.checkOngoingChildrenMultiSecretRotations(parentResourceCrn, multiSecretType.value(), userCrn));
        }
        if (multiSecretType.childSecretTypesByDescriptor().containsKey(CrnResourceDescriptor.DATALAKE)) {
            sdxRotationOngoing = ThreadBasedUserCrnProvider.doAsInternalActor(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> sdxRotationEndpoint.checkOngoingMultiSecretChildrenRotations(parentResourceCrn, multiSecretType.value(), userCrn));
        }
        return distroxRotationOngoing || sdxRotationOngoing;
    }

    @Override
    public void markChildren(String parentResourceCrn, MultiSecretType multiSecretType) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (multiSecretType.childSecretTypesByDescriptor().containsKey(CrnResourceDescriptor.DATAHUB)) {
            ThreadBasedUserCrnProvider.doAsInternalActor(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> distroXV1RotationEndpoint.markMultiClusterChildrenResources(parentResourceCrn, multiSecretType.value(), userCrn));
        }
        if (multiSecretType.childSecretTypesByDescriptor().containsKey(CrnResourceDescriptor.DATALAKE)) {
            ThreadBasedUserCrnProvider.doAsInternalActor(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> sdxRotationEndpoint.markMultiClusterChildrenResources(parentResourceCrn, multiSecretType.value(), userCrn));
        }
    }
}
