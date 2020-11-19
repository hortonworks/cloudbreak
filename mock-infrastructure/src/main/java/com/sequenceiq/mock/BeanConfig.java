package com.sequenceiq.mock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializer;

@Configuration
public class BeanConfig {

    @Bean
    public Gson gson(GsonBuilder gsonBuilder) {
        return gsonBuilder
                .registerTypeHierarchyAdapter(JsonNode.class,
                        (JsonSerializer<JsonNode>) (src, typeOfSrc, context) -> new JsonParser().parse(src.toString()))
                .create();
    }
}
