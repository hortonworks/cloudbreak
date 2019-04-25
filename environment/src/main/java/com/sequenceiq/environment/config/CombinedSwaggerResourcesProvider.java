package com.sequenceiq.environment.config;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.model.EnvironmentApi;

import springfox.documentation.swagger.web.InMemorySwaggerResourcesProvider;
import springfox.documentation.swagger.web.SwaggerResource;
import springfox.documentation.swagger.web.SwaggerResourcesProvider;

@Primary
@Component
public class CombinedSwaggerResourcesProvider implements SwaggerResourcesProvider {

    @Resource
    private InMemorySwaggerResourcesProvider inMemorySwaggerResourcesProvider;

    @Override
    public List<SwaggerResource> get() {

        SwaggerResource jerseySwaggerResource = new SwaggerResource();
        jerseySwaggerResource.setLocation(EnvironmentApi.API_ROOT_CONTEXT + "/swagger.json");
        jerseySwaggerResource.setSwaggerVersion("2.0");
        jerseySwaggerResource.setName("Environment API");

        return Stream.concat(Stream.of(jerseySwaggerResource), inMemorySwaggerResourcesProvider.get().stream()).collect(Collectors.toList());
    }

}