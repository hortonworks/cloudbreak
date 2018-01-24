package com.sequenceiq.cloudbreak.converter.v2.cli;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class StackRepoDetailsToStackRepoDetailsJsonConverter
        extends AbstractConversionServiceAwareConverter<StackRepoDetails, AmbariStackDetailsJson> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackRepoDetailsToStackRepoDetailsJsonConverter.class);

    private static final String REDHAT_6 = "redhat6";

    private static final String REDHAT_7 = "redhat7";

    @Override
    public AmbariStackDetailsJson convert(StackRepoDetails source) {
        AmbariStackDetailsJson ambariStackDetailsJson = new AmbariStackDetailsJson();

        Map<String, String> stack = source.getStack();
        ambariStackDetailsJson.setStackRepoId(stack.get(StackRepoDetails.REPO_ID_TAG));
        ambariStackDetailsJson.setStack(stack.get(StackRepoDetails.REPO_ID_TAG));
        ambariStackDetailsJson.setRepositoryVersion(stack.get(StackRepoDetails.REPOSITORY_VERSION));
        ambariStackDetailsJson.setVersionDefinitionFileUrl(stack.get(StackRepoDetails.CUSTOM_VDF_REPO_KEY));
        ambariStackDetailsJson.setMpackUrl(stack.get(StackRepoDetails.MPACK_TAG));
        if (stack.containsKey(REDHAT_6)) {
            ambariStackDetailsJson.setOs(REDHAT_6);
            ambariStackDetailsJson.setStackBaseURL(stack.get(REDHAT_6));
        } else if (stack.containsKey(REDHAT_7)) {
            ambariStackDetailsJson.setOs(REDHAT_7);
            ambariStackDetailsJson.setStackBaseURL(stack.get(REDHAT_7));
        }

        Map<String, String> util = source.getUtil();
        ambariStackDetailsJson.setUtilsRepoId(util.get(StackRepoDetails.REPO_ID_TAG));
        if (util.containsKey(REDHAT_6)) {
            ambariStackDetailsJson.setUtilsBaseURL(util.get(REDHAT_6));
        } else if (util.containsKey(REDHAT_7)) {
            ambariStackDetailsJson.setUtilsBaseURL(util.get(REDHAT_7));
        }

        ambariStackDetailsJson.setEnableGplRepo(source.isEnableGplRepo());
        ambariStackDetailsJson.setVerify(source.isVerify());
        ambariStackDetailsJson.setVersion(source.getHdpVersion());
        return ambariStackDetailsJson;
    }
}
