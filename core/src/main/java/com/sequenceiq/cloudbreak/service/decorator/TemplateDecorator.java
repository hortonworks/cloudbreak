package com.sequenceiq.cloudbreak.service.decorator;

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Suppliers;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;

@Component
public class TemplateDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateDecorator.class);

    @Inject
    private CloudParameterService cloudParameterService;

    private final Supplier<Map<Platform, Collection<VmType>>> virtualMachines =
            Suppliers.memoize(() -> cloudParameterService.getVmtypes(null, true).getVirtualMachines());

    private final Supplier<Map<Platform, Map<String, VolumeParameterType>>> diskMappings =
            Suppliers.memoize(() -> cloudParameterService.getDiskTypes().getDiskMappings());

    public Template decorate(Template subject) {
        Supplier<VolumeParameterConfig> config = Suppliers.memoize(() -> {
            try {
                Platform platform = Platform.platform(subject.cloudPlatform());
                VmType vmType = virtualMachines.get().get(platform).stream().filter(curr -> curr.value().equals(subject.getInstanceType())).findFirst().get();
                Map<String, VolumeParameterType> map = diskMappings.get().get(platform);
                VolumeParameterType volumeParameterType = map.get(subject.getVolumeType());

                return vmType.getVolumeParameterbyVolumeParameterType(volumeParameterType);
            } catch (NoSuchElementException ignored) {
                LOGGER.info("No VolumeParameterConfig found, which might be normal for platforms like OpenStack");
                return VolumeParameterConfig.EMPTY;
            }
        });

        if (config.get().volumeParameterType() != null) {
            if (subject.getVolumeCount() == null) {
                subject.setVolumeCount(config.get().maximumNumber());
            }
            if (subject.getVolumeSize() == null) {
                subject.setVolumeSize(config.get().maximumSize());
            }
        }

        return subject;
    }
}
