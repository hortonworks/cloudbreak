package com.sequenceiq.cloudbreak.converter.v4.stacks.cli;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.repository.RepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.StackRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.mpack.ManagementPackDetailsV4Request;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class StackRepoDetailsToStackRepositoryV4RequestConverter
        extends AbstractConversionServiceAwareConverter<StackRepoDetails, StackRepositoryV4Request> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackRepoDetailsToStackRepositoryV4RequestConverter.class);

    private static final String REDHAT_6 = "redhat6";

    private static final String REDHAT_7 = "redhat7";

    private static final String DEBIAN_9 = "debian9";

    private static final String UBUNTU_16 = "ubuntu16";

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public StackRepositoryV4Request convert(StackRepoDetails source) {
        StackRepositoryV4Request stackRepositoryV4Request = new StackRepositoryV4Request();

        Map<String, String> stack = source.getStack();
        stackRepositoryV4Request.setRepoId(stack.get(StackRepoDetails.REPO_ID_TAG));
        stackRepositoryV4Request.setStack(stack.get(StackRepoDetails.REPO_ID_TAG));
        updateRepository(source, stackRepositoryV4Request);
        stackRepositoryV4Request.setVersionDefinitionFileUrl(stack.get(StackRepoDetails.CUSTOM_VDF_REPO_KEY));

        Map<String, String> util = source.getUtil();
        stackRepositoryV4Request.setUtilsRepoId(util.get(StackRepoDetails.REPO_ID_TAG));
        if (util.containsKey(REDHAT_6)) {
            stackRepositoryV4Request.setUtilsBaseURL(util.get(REDHAT_6));
        } else if (util.containsKey(REDHAT_7)) {
            stackRepositoryV4Request.setUtilsBaseURL(util.get(REDHAT_7));
        } else if (util.containsKey(DEBIAN_9)) {
            stackRepositoryV4Request.setUtilsBaseURL(util.get(DEBIAN_9));
        } else if (util.containsKey(UBUNTU_16)) {
            stackRepositoryV4Request.setUtilsBaseURL(util.get(UBUNTU_16));
        }

        stackRepositoryV4Request.setEnableGplRepo(source.isEnableGplRepo());
        stackRepositoryV4Request.setVerify(source.isVerify());
        stackRepositoryV4Request.setVersion(source.getMajorHdpVersion());
        if (!source.getMpacks().isEmpty()) {
            stackRepositoryV4Request.setMpacks(converterUtil.convertAll(source.getMpacks(), ManagementPackDetailsV4Request.class));
            Optional<ManagementPackComponent> stackDefaultMpack = source.getMpacks().stream().filter(ManagementPackComponent::isStackDefault).findFirst();
            stackDefaultMpack.ifPresent(mp -> stackRepositoryV4Request.setMpackUrl(mp.getMpackUrl()));
        }
        return stackRepositoryV4Request;
    }

    private void updateRepository(StackRepoDetails source, StackRepositoryV4Request response) {
        RepositoryV4Request ret = new RepositoryV4Request();
        Map<String, String> stack = source.getStack();
        String repositoryVersion = stack.get(StackRepoDetails.REPOSITORY_VERSION);
        if (repositoryVersion != null) {
            ret.setVersion(repositoryVersion);
        }
        if (stack.containsKey(REDHAT_6)) {
            response.setOs(REDHAT_6);
            ret.setBaseUrl(stack.get(REDHAT_6));
        } else if (stack.containsKey(REDHAT_7)) {
            response.setOs(REDHAT_7);
            ret.setBaseUrl(stack.get(REDHAT_7));
        } else if (stack.containsKey(DEBIAN_9)) {
            response.setOs(DEBIAN_9);
            ret.setBaseUrl(stack.get(DEBIAN_9));
        } else if (stack.containsKey(UBUNTU_16)) {
            response.setOs(UBUNTU_16);
            ret.setBaseUrl(stack.get(UBUNTU_16));
        }
        response.setRepository(ret);
    }
}
