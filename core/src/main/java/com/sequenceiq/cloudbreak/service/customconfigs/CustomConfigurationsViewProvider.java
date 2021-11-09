package com.sequenceiq.cloudbreak.service.customconfigs;

import com.sequenceiq.cloudbreak.domain.CustomConfigurationProperty;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;
import com.sequenceiq.cloudbreak.template.views.CustomConfigurationPropertyView;
import com.sequenceiq.cloudbreak.template.views.CustomConfigurationsView;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CustomConfigurationsViewProvider {

    public CustomConfigurationsView getCustomConfigurationsView(@Nonnull CustomConfigurations customConfigurations) {
        Set<CustomConfigurationPropertyView> configsView = customConfigurations.getConfigurations()
                .stream()
                .map(this::getCustomConfigurationPropertyView)
                .collect(Collectors.toSet());
        return new CustomConfigurationsView(customConfigurations.getName(), customConfigurations.getCrn(),
                customConfigurations.getRuntimeVersion(), configsView);
    }

    public CustomConfigurationPropertyView getCustomConfigurationPropertyView(@Nonnull CustomConfigurationProperty customConfigurationProperty) {
        return new CustomConfigurationPropertyView(customConfigurationProperty.getName(),
                customConfigurationProperty.getValue(),
                customConfigurationProperty.getRoleType(),
                customConfigurationProperty.getServiceType());
    }
}
