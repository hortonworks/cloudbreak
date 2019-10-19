package com.sequenceiq.cloudbreak.service.blueprint;

import static com.sequenceiq.cloudbreak.cluster.api.ClusterApi.CLOUDERA_MANAGER;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateValidator;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.template.validation.BlueprintValidator;

@Component
public class BlueprintValidatorFactory {

    @Inject
    private BlueprintUtils blueprintUtils;

    @Inject
    private CmTemplateValidator cmTemplateValidator;

    public BlueprintValidator createBlueprintValidator(Blueprint blueprint) {
        String variant = blueprintUtils.getBlueprintVariant(blueprint.getBlueprintText());
        switch (variant) {
            case CLOUDERA_MANAGER:
                return cmTemplateValidator;
            default:
                throw new NotImplementedException("BlueprintValidator for " + variant);
        }
    }
}
