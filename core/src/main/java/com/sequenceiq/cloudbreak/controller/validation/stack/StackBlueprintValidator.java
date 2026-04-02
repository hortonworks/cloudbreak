package com.sequenceiq.cloudbreak.controller.validation.stack;

import static com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.REPOSITORY_VERSION;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_13_2_100;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.dataviz.DatavizRoles;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;

@Component
public class StackBlueprintValidator {

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    public void validateComponentsByRuntime(StackV4Request stackV4Request, StackDtoDelegate stack, Image targetImage) {
        Optional<String> runtimeVersion = Optional.ofNullable(targetImage.getStackDetails())
                .map(ImageStackDetails::getRepo)
                .map(StackRepoDetails::getStack)
                .map(s -> s.get(REPOSITORY_VERSION))
                .or(() -> Optional.ofNullable(stackV4Request.getCluster())
                        .flatMap(cluster -> Optional.ofNullable(cluster.getCm()))
                        .flatMap(cm -> Optional.ofNullable(cm.getRepository()))
                        .flatMap(repo -> Optional.ofNullable(repo.getVersion())));
        Blueprint blueprint = blueprintService.get(stack.getBlueprint().getId());
        CmTemplateProcessor processor = cmTemplateProcessorFactory.get(blueprint.getBlueprintJsonText());
        if (componentPresented(processor, DatavizRoles.DATAVIZ) && runtimeVersion.isPresent()) {
            String version = runtimeVersion.get().split("-")[0];
            if (!isVersionNewerOrEqualThanLimited(version, CLOUDERA_STACK_VERSION_7_13_2_100)) {
                throw new BadRequestException("DATAVIZ clusters only supperted if Cloudera Manager is >= 7.13.2.100");
            }
        }
    }

    private boolean componentPresented(CmTemplateProcessor processor, String component) {
        return processor.getAllComponents()
                .stream()
                .anyMatch(e -> e.getService().equalsIgnoreCase(component));
    }

}
