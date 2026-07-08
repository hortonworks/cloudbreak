package com.sequenceiq.cloudbreak.controller.validation.stack;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_3_2_100;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.dataviz.DatavizRoles.DATAVIZ;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.opensearch.OpenSearchRoles.OPENSEARCH;

import java.util.Locale;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.util.CdhVersionProvider;

@Component
public class StackBlueprintValidator {

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    public void validateComponentsByRuntime(StackV4Request stackV4Request, StackDtoDelegate stack, Image targetImage) {
        Optional<String> cdhVersion = Optional.ofNullable(targetImage.getStackDetails())
                .map(ImageStackDetails::getRepo)
                .map(com.sequenceiq.cloudbreak.cloud.model.catalog.StackRepoDetails::getStack)
                .map(s -> s.get(StackRepoDetails.REPOSITORY_VERSION))
                .map(CdhVersionProvider::getCdhFullVersionFromVersionString)
                .or(() -> Optional.ofNullable(stackV4Request.getCluster())
                        .map(ClusterV4Request::getCm)
                        .map(ClouderaManagerV4Request::getProducts)
                        .flatMap(products -> products.stream()
                                .filter(e -> "CDH".equalsIgnoreCase(e.getName()))
                                .findFirst())
                        .map(ClouderaManagerProductV4Request::getVersion)
                        .map(CdhVersionProvider::getCdhFullVersionFromVersionString));
        Blueprint blueprint = blueprintService.get(stack.getBlueprint().getId());
        CmTemplateProcessor processor = cmTemplateProcessorFactory.get(blueprint.getBlueprintJsonText());

        if (cdhVersion.isPresent()) {
            validateComponentVersion(processor, DATAVIZ, cdhVersion.get(), CLOUDERA_STACK_VERSION_7_3_2_100);
            validateComponentVersion(processor, OPENSEARCH, cdhVersion.get(), CLOUDERA_STACK_VERSION_7_3_2_100);
        }
    }

    private void validateComponentVersion(CmTemplateProcessor processor, String component, String runtimeVersion, Versioned minimumVersion) {
        if (componentPresented(processor, component) && StringUtils.hasText(runtimeVersion)) {
            if (!isVersionNewerOrEqualThanLimited(runtimeVersion, minimumVersion)) {
                throw new BadRequestException(
                        String.format("%s clusters only supported if Cloudera Manager is >= %s",
                                component.toUpperCase(Locale.ROOT),
                                minimumVersion.getVersion()
                        )
                );
            }
        }
    }

    private boolean componentPresented(CmTemplateProcessor processor, String component) {
        return processor.getAllComponents()
                .stream()
                .anyMatch(e -> e.getService().equalsIgnoreCase(component));
    }

}
