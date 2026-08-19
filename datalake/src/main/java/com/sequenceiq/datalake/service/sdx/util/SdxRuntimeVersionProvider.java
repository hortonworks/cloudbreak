package com.sequenceiq.datalake.service.sdx.util;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.BaseStackDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.BaseStackRepoDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerProductV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerV4Response;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.util.CdhVersionProvider;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;

public class SdxRuntimeVersionProvider {

    private SdxRuntimeVersionProvider() {
    }

    public static String getRuntime(ImageV4Response imageV4Response, SdxClusterRequest sdxClusterRequest, StackV4Request stackV4Request,
            String defaultRuntimeVersion) {
        return Optional.ofNullable(getIfNotNull(imageV4Response, ImageV4Response::getVersion))
                .or(() -> Optional.ofNullable(sdxClusterRequest.getRuntime()))
                .or(() -> extractRuntimeFromCdhProductVersion(stackV4Request))
                .orElse(defaultRuntimeVersion);
    }

    public static String getServicePackQualifiedRuntimeVersion(ImageV4Response imageV4Response, StackV4Request stackV4Request, String runtimeVersion) {
        return extractRepositoryVersionFromPrewarmedImage(imageV4Response)
                .or(() -> extractCdhProductVersion(stackV4Request))
                .map(CdhVersionProvider::getCdhFullVersionFromVersionString)
                .filter(StringUtils::isNotBlank)
                .orElse(runtimeVersion);
    }

    public static String getServicePackQualifiedRuntimeVersion(StackV4Response stackV4Response, String runtime) {
        return Optional.ofNullable(stackV4Response)
                .map(StackV4Response::getCluster)
                .map(ClusterV4Response::getCm)
                .map(ClouderaManagerV4Response::getProducts)
                .stream()
                .flatMap(List::stream)
                .filter(product -> "CDH".equalsIgnoreCase(product.getName()))
                .map(ClouderaManagerProductV4Response::getVersion)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .map(CdhVersionProvider::getCdhFullVersionFromVersionString)
                .filter(StringUtils::isNotBlank)
                .orElse(runtime);
    }

    private static Optional<String> extractRuntimeFromCdhProductVersion(StackV4Request stackV4Request) {
        return getClouderaManagerProductV4RequestStream(stackV4Request)
                .map(product -> StringUtils.substringBefore(product.getVersion(), "-"))
                .findFirst();
    }

    private static Optional<String> extractRepositoryVersionFromPrewarmedImage(ImageV4Response imageV4Response) {
        return Optional.ofNullable(imageV4Response)
                .map(ImageV4Response::getStackDetails)
                .map(BaseStackDetailsV4Response::getRepository)
                .map(BaseStackRepoDetailsV4Response::getStack)
                .map(stack -> stack.get(StackRepoDetails.REPOSITORY_VERSION))
                .filter(StringUtils::isNotBlank);
    }

    private static Optional<String> extractCdhProductVersion(StackV4Request stackV4Request) {
        return getClouderaManagerProductV4RequestStream(stackV4Request)
                .map(ClouderaManagerProductV4Request::getVersion)
                .filter(StringUtils::isNotBlank)
                .findFirst();
    }

    private static Stream<ClouderaManagerProductV4Request> getClouderaManagerProductV4RequestStream(StackV4Request stackV4Request) {
        return Optional.ofNullable(stackV4Request)
                .map(StackV4Request::getCluster)
                .map(ClusterV4Request::getCm)
                .map(ClouderaManagerV4Request::getProducts)
                .stream()
                .flatMap(List::stream)
                .filter(product -> "CDH".equalsIgnoreCase(product.getName()));
    }
}
