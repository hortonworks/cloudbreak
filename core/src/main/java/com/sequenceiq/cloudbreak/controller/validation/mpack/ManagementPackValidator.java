package com.sequenceiq.cloudbreak.controller.validation.mpack;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.StackRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.mpack.ManagementPackDetailsV4Request;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.ManagementPack;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.mpack.ManagementPackService;

@Component
public class ManagementPackValidator {
    @Inject
    private ManagementPackService mpackService;

    public void validateMpacks(AmbariV4Request ambari, Workspace workspace) {
        if (ambari == null || ambari.getStackRepository() == null) {
            return;
        }
        StackRepositoryV4Request stackDetails = ambari.getStackRepository();
        String mpackUrl = stackDetails.getMpackUrl();
        List<ManagementPackDetailsV4Request> mpackList = stackDetails.getMpacks() != null ? stackDetails.getMpacks() : Collections.emptyList();
        Map<String, ManagementPackDetailsV4Request> mpackDetailsMap = mpackList.stream()
                .collect(Collectors.toMap(ManagementPackDetailsV4Request::getName, mp -> mp, (mp1, mp2) -> mp1));
        if (mpackDetailsMap.size() != mpackList.size()) {
            throw new BadRequestException("Mpack list contains entries with the same name");
        }
        Map<String, ManagementPack> mpackMap = mpackDetailsMap.values().stream()
                .map(mpd -> mpackService.getByNameForWorkspace(mpd.getName(), workspace))
                .collect(Collectors.toMap(ManagementPack::getMpackUrl, mp -> mp, (mp1, mp2) -> mp1));
        if (mpackMap.size() != mpackList.size()) {
            throw new BadRequestException("Mpack list contains entries with the same mpackUrl");
        }
        if (mpackMap.get(mpackUrl) != null) {
            throw new BadRequestException("Mpack list contains entries with the stackDefault mpackUrl");
        }
        validatePurgedMpacks(mpackUrl, mpackList, mpackMap);
    }

    private void validatePurgedMpacks(String mpackUrl, List<ManagementPackDetailsV4Request> mpackList, Map<String, ManagementPack> mpackMap) {
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
