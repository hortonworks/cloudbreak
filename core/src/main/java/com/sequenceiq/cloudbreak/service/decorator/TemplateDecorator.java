package com.sequenceiq.cloudbreak.service.decorator;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;

@Component
public class TemplateDecorator implements Decorator<Template> {

    @Inject
    private CloudParameterService cloudParameterService;

    private Supplier<Map<Platform, Collection<VmType>>> virtualMachines =
            Suppliers.memoize(() -> cloudParameterService.getVmtypes(true).getVirtualMachines());
    private Supplier<Map<Platform, Map<String, VolumeParameterType>>> diskMappings =
            Suppliers.memoize(() -> cloudParameterService.getDiskTypes().getDiskMappings());

    @Override
    public Template decorate(final Template subject, Object... data) {
        Supplier<VolumeParameterConfig> config = Suppliers.memoize(() -> {
            Platform platform = Platform.platform(subject.cloudPlatform());
            VmType vmType = virtualMachines.get().get(platform).stream().filter(curr -> curr.value().equals(subject.getInstanceType())).findFirst().get();
            Map<String, VolumeParameterType> map = diskMappings.get().get(platform);
            VolumeParameterType volumeParameterType = map.get(subject.getVolumeType());

            return vmType.getVolumeParameterbyVolumeParameterType(volumeParameterType);
        });

        if (subject.getVolumeCount() == null) {
            subject.setVolumeCount(config.get().maximumNumber());
        }
        if (subject.getVolumeSize() == null) {
            subject.setVolumeSize(config.get().maximumSize());
        }

        return subject;
    }
}
