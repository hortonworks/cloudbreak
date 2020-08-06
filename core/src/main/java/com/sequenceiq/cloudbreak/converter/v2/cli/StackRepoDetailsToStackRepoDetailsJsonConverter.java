package com.sequenceiq.cloudbreak.converter.v2.cli;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.service.credential.PaywallCredentialService;

@Component
public class StackRepoDetailsToStackRepoDetailsJsonConverter
        extends AbstractConversionServiceAwareConverter<StackRepoDetails, AmbariStackDetailsJson> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackRepoDetailsToStackRepoDetailsJsonConverter.class);

    private static final String REDHAT_6 = "redhat6";

    private static final String REDHAT_7 = "redhat7";

    private static final String DEBIAN_9 = "debian9";

    private static final String UBUNTU_16 = "ubuntu16";

    @Inject
    private ConversionService conversionService;

    @Inject
    private PaywallCredentialService paywallCredentialService;

    @Override
    public AmbariStackDetailsJson convert(StackRepoDetails source) {
        AmbariStackDetailsJson ambariStackDetailsJson = new AmbariStackDetailsJson();

        Map<String, String> stack = source.getStack();
        ambariStackDetailsJson.setStackRepoId(stack.get(StackRepoDetails.REPO_ID_TAG));
        ambariStackDetailsJson.setStack(stack.get(StackRepoDetails.REPO_ID_TAG));
        ambariStackDetailsJson.setRepositoryVersion(stack.get(StackRepoDetails.REPOSITORY_VERSION));
        ambariStackDetailsJson.setVersionDefinitionFileUrl(stack.get(StackRepoDetails.CUSTOM_VDF_REPO_KEY));
        if (stack.containsKey(REDHAT_6)) {
            ambariStackDetailsJson.setOs(REDHAT_6);
            ambariStackDetailsJson.setStackBaseURL(stack.get(REDHAT_6));
        } else if (stack.containsKey(REDHAT_7)) {
            ambariStackDetailsJson.setOs(REDHAT_7);
            ambariStackDetailsJson.setStackBaseURL(stack.get(REDHAT_7));
        } else if (stack.containsKey(DEBIAN_9)) {
            ambariStackDetailsJson.setOs(DEBIAN_9);
            ambariStackDetailsJson.setStackBaseURL(stack.get(DEBIAN_9));
        } else if (stack.containsKey(UBUNTU_16)) {
            ambariStackDetailsJson.setOs(UBUNTU_16);
            ambariStackDetailsJson.setStackBaseURL(stack.get(UBUNTU_16));
        }

        Map<String, String> util = source.getUtil();
        ambariStackDetailsJson.setUtilsRepoId(util.get(StackRepoDetails.REPO_ID_TAG));
        if (util.containsKey(REDHAT_6)) {
            ambariStackDetailsJson.setUtilsBaseURL(util.get(REDHAT_6));
        } else if (util.containsKey(REDHAT_7)) {
            ambariStackDetailsJson.setUtilsBaseURL(util.get(REDHAT_7));
        } else if (util.containsKey(DEBIAN_9)) {
            ambariStackDetailsJson.setUtilsBaseURL(util.get(DEBIAN_9));
        } else if (util.containsKey(UBUNTU_16)) {
            ambariStackDetailsJson.setUtilsBaseURL(util.get(UBUNTU_16));
        }

        ambariStackDetailsJson.setEnableGplRepo(source.isEnableGplRepo());
        ambariStackDetailsJson.setVerify(source.isVerify());
        ambariStackDetailsJson.setVersion(source.getMajorHdpVersion());
        if (!source.getMpacks().isEmpty()) {
            List<ManagementPackDetails> mpacks = source.getMpacks().stream().filter(mp -> !mp.isStackDefault()).map(mp -> conversionService.convert(
                    mp, ManagementPackDetails.class)).collect(Collectors.toList());
            ambariStackDetailsJson.setMpacks(mpacks);
            Optional<ManagementPackComponent> stackDefaultMpack = source.getMpacks().stream().filter(ManagementPackComponent::isStackDefault).findFirst();
            stackDefaultMpack.ifPresent(mp -> ambariStackDetailsJson.setMpackUrl(getMpackUrl(mp.getMpackUrl())));
        }
        return ambariStackDetailsJson;
    }

    private String getMpackUrl(String url) {
        return paywallCredentialService.paywallCredentialAvailable()
                ? paywallCredentialService.addCredentialForUrl(url)
                : url;
    }
}
