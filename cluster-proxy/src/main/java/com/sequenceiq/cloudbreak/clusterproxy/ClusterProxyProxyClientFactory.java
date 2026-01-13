package com.sequenceiq.cloudbreak.clusterproxy;

import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.ACTOR_CRN_HEADER;

import java.util.List;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Scope("prototype")
public class ClusterProxyProxyClientFactory {

    private final RestTemplateBuilder restTemplateBuilder;

    public ClusterProxyProxyClientFactory(ClusterProxyConfiguration clusterProxyConfiguration, RestTemplateBuilder restTemplateBuilder) {
        this.restTemplateBuilder = restTemplateBuilder
                .rootUri(clusterProxyConfiguration.getClusterProxyUrl() + clusterProxyConfiguration.getHttpProxyPath())
                .interceptors(List.of(new RequestIdProviderRequestInterceptor()))
                .requestFactory(HttpComponentsClientHttpRequestFactory.class);
    }

    public ClusterProxyProxyClient create(String clusterCrn, String serviceName, String userCrn) {
        RestTemplate restTemplate = restTemplateBuilder
                .defaultHeader(ACTOR_CRN_HEADER, userCrn)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
        return new ClusterProxyProxyClient(restTemplate, clusterCrn, serviceName);
    }
}
