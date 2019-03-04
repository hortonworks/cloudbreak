package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ambari;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.RepositoryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.stackrepository.StackRepositoryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.stackrepository.mpack.ManagementPackDetailsV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class StackRepoDetailsToStackRepositoryV4ResponseConverter
        extends AbstractConversionServiceAwareConverter<StackRepoDetails, StackRepositoryV4Response> {

    private static final String REDHAT_6 = "redhat6";

    private static final String REDHAT_7 = "redhat7";

    private static final String DEBIAN_9 = "debian9";

    private static final String UBUNTU_16 = "ubuntu16";

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public StackRepositoryV4Response convert(StackRepoDetails source) {
        StackRepositoryV4Response stackRepositoryV4Response = new StackRepositoryV4Response();

        Map<String, String> stack = source.getStack();
        String repoId = stack.get(StackRepoDetails.REPO_ID_TAG);
        stackRepositoryV4Response.setRepoId(repoId);
        stackRepositoryV4Response.setStack(getStackFromRepoId(repoId));
        updateRepository(source, stackRepositoryV4Response);
        stackRepositoryV4Response.setVersionDefinitionFileUrl(stack.get(StackRepoDetails.CUSTOM_VDF_REPO_KEY));

        Map<String, String> util = source.getUtil();
        stackRepositoryV4Response.setUtilsRepoId(util.get(StackRepoDetails.REPO_ID_TAG));
        if (util.containsKey(REDHAT_6)) {
            stackRepositoryV4Response.setUtilsBaseURL(util.get(REDHAT_6));
        } else if (util.containsKey(REDHAT_7)) {
            stackRepositoryV4Response.setUtilsBaseURL(util.get(REDHAT_7));
        } else if (util.containsKey(DEBIAN_9)) {
            stackRepositoryV4Response.setUtilsBaseURL(util.get(DEBIAN_9));
        } else if (util.containsKey(UBUNTU_16)) {
            stackRepositoryV4Response.setUtilsBaseURL(util.get(UBUNTU_16));
        }

        stackRepositoryV4Response.setEnableGplRepo(source.isEnableGplRepo());
        stackRepositoryV4Response.setVerify(source.isVerify());
        stackRepositoryV4Response.setVersion(source.getMajorHdpVersion());
        if (!source.getMpacks().isEmpty()) {
            stackRepositoryV4Response.setMpacks(converterUtil.convertAll(source.getMpacks(), ManagementPackDetailsV4Response.class));
            Optional<ManagementPackComponent> stackDefaultMpack = source.getMpacks().stream().filter(ManagementPackComponent::isStackDefault).findFirst();
            stackDefaultMpack.ifPresent(mp -> stackRepositoryV4Response.setMpackUrl(mp.getMpackUrl()));
        }
        return stackRepositoryV4Response;
    }

    private String getStackFromRepoId(String repoId) {
        String versionDelimiter = "-";
        String stackIdentifier = repoId;
        if (stackIdentifier.contains(versionDelimiter)) {
            int lastIndexOfDelimiter = stackIdentifier.lastIndexOf(versionDelimiter);
            stackIdentifier = stackIdentifier.substring(0, lastIndexOfDelimiter);
        }
        return stackIdentifier;
    }

    private void updateRepository(StackRepoDetails source, StackRepositoryV4Response response) {
        RepositoryV4Response ret = new RepositoryV4Response();
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
