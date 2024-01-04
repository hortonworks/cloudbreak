package com.sequenceiq.freeipa.service.rotation;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.rotation.MultiSecretType;
import com.sequenceiq.cloudbreak.rotation.service.multicluster.InterServiceMultiClusterRotationService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1RotationEndpoint;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXChildResourceMarkingRequest;
import com.sequenceiq.sdx.api.endpoint.SdxRotationEndpoint;
import com.sequenceiq.sdx.api.model.SdxChildResourceMarkingRequest;

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
        if (multiSecretType.getChildrenCrnDescriptors().contains(CrnResourceDescriptor.DATAHUB)) {
            distroxRotationOngoing = ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> distroXV1RotationEndpoint.checkOngoingChildrenMultiSecretRotationsByParent(parentResourceCrn, multiSecretType.value(), userCrn));
        }
        if (multiSecretType.getChildrenCrnDescriptors().contains(CrnResourceDescriptor.DATALAKE)) {
            sdxRotationOngoing = ThreadBasedUserCrnProvider.doAsInternalActor(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> sdxRotationEndpoint.checkOngoingMultiSecretChildrenRotationsByParent(parentResourceCrn, multiSecretType.value(), userCrn));
        }
        return distroxRotationOngoing || sdxRotationOngoing;
    }

    @Override
    public void markChildren(String parentResourceCrn, MultiSecretType multiSecretType) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (multiSecretType.getChildrenCrnDescriptors().contains(CrnResourceDescriptor.DATALAKE)) {
            SdxChildResourceMarkingRequest request = getSdxRequest(parentResourceCrn, multiSecretType);
            ThreadBasedUserCrnProvider.doAsInternalActor(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> sdxRotationEndpoint.markMultiClusterChildrenResourcesByParent(request, userCrn));
        }
        if (multiSecretType.getChildrenCrnDescriptors().contains(CrnResourceDescriptor.DATAHUB)) {
            DistroXChildResourceMarkingRequest request = getDistroxRequest(parentResourceCrn, multiSecretType);
            ThreadBasedUserCrnProvider.doAsInternalActor(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> distroXV1RotationEndpoint.markMultiClusterChildrenResourcesByParent(request, userCrn));
        }
    }

    private static DistroXChildResourceMarkingRequest getDistroxRequest(String parentResourceCrn, MultiSecretType multiSecretType) {
        DistroXChildResourceMarkingRequest request = new DistroXChildResourceMarkingRequest();
        request.setSecret(multiSecretType.value());
        request.setParentCrn(parentResourceCrn);
        return request;
    }

    private static SdxChildResourceMarkingRequest getSdxRequest(String parentResourceCrn, MultiSecretType multiSecretType) {
        SdxChildResourceMarkingRequest request = new SdxChildResourceMarkingRequest();
        request.setSecret(multiSecretType.value());
        request.setParentCrn(parentResourceCrn);
        return request;
    }
}
