package com.sequenceiq.cloudbreak.service.blueprint;

import static com.sequenceiq.cloudbreak.cluster.api.ClusterApi.AMBARI;
import static com.sequenceiq.cloudbreak.cluster.api.ClusterApi.CLOUDERA_MANAGER;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.blueprint.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.blueprint.validation.AmbariBlueprintValidator;
import com.sequenceiq.cloudbreak.template.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateValidator;
import com.sequenceiq.cloudbreak.domain.Blueprint;

@Component
public class BlueprintValidatorFactory {

    @Inject
    private BlueprintUtils blueprintUtils;

    @Inject
    private AmbariBlueprintValidator ambariBlueprintValidator;

    @Inject
    private CmTemplateValidator cmTemplateValidator;

    public BlueprintValidator createBlueprintValidator(Blueprint blueprint) {
        String variant = blueprintUtils.getBlueprintVariant(blueprint.getBlueprintText());
        switch (variant) {
            case AMBARI:
                return ambariBlueprintValidator;
            case CLOUDERA_MANAGER:
                return cmTemplateValidator;
            default:
                throw new NotImplementedException("BlueprintValidator for " + variant);
        }
    }
}
