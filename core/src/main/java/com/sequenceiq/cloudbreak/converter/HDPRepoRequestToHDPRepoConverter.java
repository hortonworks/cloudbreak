package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.controller.BadRequestException;

@Component
public class HDPRepoRequestToHDPRepoConverter extends AbstractConversionServiceAwareConverter<AmbariStackDetailsJson, StackRepoDetails> {

    private static final String REDHAT_6 = "redhat6";

    private static final String REDHAT_7 = "redhat7";

    @Override
    public StackRepoDetails convert(AmbariStackDetailsJson source) {
        StackRepoDetails repo = new StackRepoDetails();
        Map<String, String> stack = new HashMap<>();
        Map<String, String> util = new HashMap<>();
        validateRepoFieldsExistence(source);

        stack.put("repoid", source.getStackRepoId());
        util.put("repoid", source.getUtilsRepoId());

        String stackBaseURL = source.getStackBaseURL();
        String utilsBaseURL = source.getUtilsBaseURL();
        if (source.getOs() == null) {
            stack.put(REDHAT_6, stackBaseURL);
            stack.put(REDHAT_7, stackBaseURL);
            util.put(REDHAT_6, utilsBaseURL);
            util.put(REDHAT_7, utilsBaseURL);
        } else {
            stack.put(source.getOs(), stackBaseURL);
            util.put(source.getOs(), utilsBaseURL);
        }

        if (!StringUtils.isEmpty(source.getRepositoryVersion())) {
            stack.put(StackRepoDetails.REPOSITORY_VERSION, source.getRepositoryVersion());
            stack.put("repoid", source.getStack());
        }
        if (!StringUtils.isEmpty(source.getVersionDefinitionFileUrl())) {
            stack.put(StackRepoDetails.CUSTOM_VDF_REPO_KEY, source.getVersionDefinitionFileUrl());
        }

        if (!StringUtils.isEmpty(source.getMpackUrl())) {
            stack.put(StackRepoDetails.MPACK_TAG, source.getMpackUrl());
        }

        repo.setStack(stack);
        repo.setUtil(util);
        repo.setEnableGplRepo(source.isEnableGplRepo());
        repo.setVerify(source.getVerify());
        repo.setHdpVersion(source.getVersion());
        return repo;
    }

    private void validateRepoFieldsExistence(AmbariStackDetailsJson source) {
        boolean vdfRequiredFieldsExists = !StringUtils.isEmpty(source.getRepositoryVersion())
                && !StringUtils.isEmpty(source.getVersionDefinitionFileUrl());

        boolean customBaseRepoRequiredFieldsExists = !StringUtils.isEmpty(source.getStackBaseURL())
                && !StringUtils.isEmpty(source.getStackRepoId())
                && !StringUtils.isEmpty(source.getUtilsBaseURL())
                && !StringUtils.isEmpty(source.getUtilsRepoId());

        if (!vdfRequiredFieldsExists && !customBaseRepoRequiredFieldsExists) {
            String msg = "The 'repositoryVersion', 'versionDefinitionFileUrl' or "
                    + "'stackBaseURL', 'stackRepoId', 'utilsBaseUrl', 'utilsRepoId' fields must be specified!";
            throw new BadRequestException(msg);
        }
    }
}
