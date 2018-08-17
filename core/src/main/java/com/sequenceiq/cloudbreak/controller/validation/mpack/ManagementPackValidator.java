package com.sequenceiq.cloudbreak.controller.validation.mpack;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackDetails;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.ManagementPack;
import com.sequenceiq.cloudbreak.service.mpack.ManagementPackService;

@Component
public class ManagementPackValidator {
    @Inject
    private ManagementPackService mpackService;

    public void validateMpacks(ClusterRequest clusterRequest, IdentityUser user) {
        AmbariStackDetailsJson stackDetails = clusterRequest.getAmbariStackDetails();
        if (stackDetails == null) {
            return;
        }
        String mpackUrl = stackDetails.getMpackUrl();
        List<ManagementPackDetails> mpackList = stackDetails.getMpacks();
        Map<String, ManagementPackDetails> mpackDetailsMap = stackDetails.getMpacks().stream()
                .collect(Collectors.toMap(ManagementPackDetails::getName, mp -> mp, (mp1, mp2) -> mp1));
        if (mpackDetailsMap.size() != mpackList.size()) {
            throw new BadRequestException("Mpack list contains entries with the same name");
        }
        Map<String, ManagementPack> mpackMap = mpackDetailsMap.values().stream()
                .map(mpd -> mpackService.getByNameFromUsersDefaultOrganization(mpd.getName()))
                .collect(Collectors.toMap(ManagementPack::getMpackUrl, mp -> mp, (mp1, mp2) -> mp1));
        if (mpackMap.size() != mpackList.size()) {
            throw new BadRequestException("Mpack list contains entries with the same mpackUrl");
        }
        if (mpackMap.get(mpackUrl) != null) {
            throw new BadRequestException("Mpack list contains entries with the stackDefault mpackUrl");
        }
        List<ManagementPack> purgedMpacks = mpackMap.values().stream()
                .filter(ManagementPack::isPurge)
                .collect(Collectors.toList());
        if (purgedMpacks.size() > 1) {
            throw new BadRequestException("Mpack list contains more than one entries with purge option");
        }
        if (purgedMpacks.size() == 1) {
            if (StringUtils.isNoneEmpty(mpackUrl)) {
                throw new BadRequestException("Mpack list cannot contain mpack with purge option if stack default mpackurl is given");
            } else if (mpackList.size() > 1) {
                throw new BadRequestException("Mpack list cannot contain purged and non purged mpacks together");
            }
        }
    }
}
