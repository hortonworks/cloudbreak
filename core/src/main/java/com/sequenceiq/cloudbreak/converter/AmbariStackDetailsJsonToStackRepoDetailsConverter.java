package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;

@Component
public class AmbariStackDetailsJsonToStackRepoDetailsConverter extends AbstractConversionServiceAwareConverter<AmbariStackDetailsJson, StackRepoDetails> {
    private static final String REDHAT_6 = "redhat6";

    private static final String REDHAT_7 = "redhat7";

    private static final String DEBIAN_9 = "debian9";

    private static final String UBUNTU_16 = "ubuntu16";

    @Inject
    private ConversionService conversionService;

    @Override
    public StackRepoDetails convert(AmbariStackDetailsJson source) {
        StackRepoDetails repo = new StackRepoDetails();
        Map<String, String> stack = new HashMap<>();
        Map<String, String> util = new HashMap<>();

        boolean baseRepoRequiredFieldsExists = isBaseRepoRequiredFieldsExists(source);

        stack.put(StackRepoDetails.REPO_ID_TAG, source.getStackRepoId());
        util.put(StackRepoDetails.REPO_ID_TAG, source.getUtilsRepoId());

        if (baseRepoRequiredFieldsExists) {
            String stackBaseURL = source.getStackBaseURL();
            String utilsBaseURL = source.getUtilsBaseURL();
            if (source.getOs() == null) {
                stack.put(REDHAT_6, stackBaseURL);
                stack.put(REDHAT_7, stackBaseURL);
                stack.put(DEBIAN_9, stackBaseURL);
                stack.put(UBUNTU_16, stackBaseURL);
                util.put(REDHAT_6, utilsBaseURL);
                util.put(REDHAT_7, utilsBaseURL);
                util.put(DEBIAN_9, utilsBaseURL);
                util.put(UBUNTU_16, utilsBaseURL);
            } else {
                stack.put(source.getOs(), stackBaseURL);
                util.put(source.getOs(), utilsBaseURL);
            }
        }

        if (!StringUtils.isEmpty(source.getRepositoryVersion())) {
            stack.put(StackRepoDetails.REPOSITORY_VERSION, source.getRepositoryVersion());
            stack.put(StackRepoDetails.REPO_ID_TAG, source.getStack());
        }

        stack.computeIfAbsent(StackRepoDetails.REPO_ID_TAG, k -> source.getStack());

        if (!StringUtils.isEmpty(source.getVersionDefinitionFileUrl())) {
            stack.put(StackRepoDetails.CUSTOM_VDF_REPO_KEY, source.getVersionDefinitionFileUrl());
        }

        if (!source.getMpacks().isEmpty()) {
            repo.setMpacks(source.getMpacks().stream().map(rmpack -> conversionService.convert(rmpack, ManagementPackComponent.class))
                    .collect(Collectors.toList()));
        }
        if (!StringUtils.isEmpty(source.getMpackUrl())) {
            ManagementPackComponent mpack = new ManagementPackComponent();
            mpack.setMpackUrl(source.getMpackUrl());
            mpack.setStackDefault(true);
            mpack.setPreInstalled(false);
            repo.getMpacks().add(mpack);
        }
        repo.setStack(stack);
        repo.setUtil(util);
        repo.setEnableGplRepo(source.isEnableGplRepo());
        repo.setVerify(Boolean.TRUE.equals(source.getVerify()));
        repo.setHdpVersion(source.getVersion());
        return repo;
    }

    public boolean isBaseRepoRequiredFieldsExists(AmbariStackDetailsJson source) {
        return Stream.of(source.getStackRepoId(), source.getStackBaseURL(), source.getUtilsRepoId(), source.getUtilsBaseURL())
                    .noneMatch(StringUtils::isEmpty);
    }

    public boolean isVdfRequiredFieldsExists(AmbariStackDetailsJson source) {
        return Stream.of(source.getRepositoryVersion(), source.getVersionDefinitionFileUrl()).noneMatch(StringUtils::isEmpty);
    }
}
