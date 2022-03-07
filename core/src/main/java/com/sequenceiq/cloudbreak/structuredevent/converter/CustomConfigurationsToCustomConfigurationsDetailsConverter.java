package com.sequenceiq.cloudbreak.structuredevent.converter;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.CustomConfigurationProperty;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;
import com.sequenceiq.cloudbreak.structuredevent.event.CustomConfigurationsDetails;

@Component
public class CustomConfigurationsToCustomConfigurationsDetailsConverter {

    public CustomConfigurationsDetails convert(CustomConfigurations customConfigurations) {
        CustomConfigurationsDetails customConfigurationsDetails = new CustomConfigurationsDetails();
        customConfigurationsDetails.setId(customConfigurations.getId());
        customConfigurationsDetails.setCustomConfigurationsName(customConfigurations.getName());
        if (customConfigurations.getConfigurations() != null) {
            customConfigurationsDetails.setServices(getServicesList(customConfigurations.getConfigurations()));
            customConfigurationsDetails.setRoles(getRolesList(customConfigurations.getConfigurations()));
        }
        customConfigurationsDetails.setRuntimeVersion(customConfigurations.getRuntimeVersion());
        return customConfigurationsDetails;
    }

    private List<String> getServicesList(Collection<CustomConfigurationProperty> properties) {
        return properties.stream()
                .map(CustomConfigurationProperty::getServiceType)
                .collect(Collectors.toList());
    }

    private List<String> getRolesList(Collection<CustomConfigurationProperty> properties) {
            return properties.stream()
                    .map(CustomConfigurationProperty::getRoleType)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
    }
}
