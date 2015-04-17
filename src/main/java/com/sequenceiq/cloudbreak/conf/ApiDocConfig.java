package com.sequenceiq.cloudbreak.conf;

import com.google.api.client.util.Charsets;
import com.google.common.io.Resources;
import com.mangofactory.swagger.configuration.SpringSwaggerConfig;
import com.mangofactory.swagger.models.dto.ApiInfo;
import com.mangofactory.swagger.models.dto.ResponseMessage;
import com.mangofactory.swagger.ordering.ResourceListingPositionalOrdering;
import com.mangofactory.swagger.plugin.EnableSwagger;
import com.mangofactory.swagger.plugin.SwaggerSpringMvcPlugin;
import com.sequenceiq.cloudbreak.domain.CbUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableSwagger
public class ApiDocConfig {

    @Autowired
    private SpringSwaggerConfig springSwaggerConfig;

    @Bean
    public SwaggerSpringMvcPlugin customImplementation() throws IOException {
        Map<HttpStatus, ResponseMessage> responseMessages = getResponseMessages();
        return new SwaggerSpringMvcPlugin(this.springSwaggerConfig)
                .apiInfo(apiInfo())
                .includePatterns(".*stack.*", ".*cluster.*", ".*blueprint.*", ".*template.*",
                        ".*credential.*", ".*recipe.*", ".*usage.*", ".*event.*")
                .ignoredParameterTypes(CbUser.class, ModelMap.class, View.class, ModelAndView.class)
                .apiListingReferenceOrdering(new ResourceListingPositionalOrdering())
                .directModelSubstitute(ModelAndView.class, OutputStream.class)
                .globalResponseMessage(RequestMethod.GET, Arrays.asList(
                        new ResponseMessage(HttpStatus.OK.value(), "Resource retrieved successfully", null),
                        responseMessages.get(HttpStatus.UNAUTHORIZED),
                        responseMessages.get(HttpStatus.NOT_ACCEPTABLE),
                        responseMessages.get(HttpStatus.FORBIDDEN),
                        responseMessages.get(HttpStatus.NOT_FOUND),
                        responseMessages.get(HttpStatus.INTERNAL_SERVER_ERROR)
                )).globalResponseMessage(RequestMethod.POST, Arrays.asList(
                        new ResponseMessage(HttpStatus.OK.value(), "Success",  null),
                        responseMessages.get(HttpStatus.CREATED),
                        responseMessages.get(HttpStatus.BAD_REQUEST),
                        responseMessages.get(HttpStatus.UNAUTHORIZED),
                        responseMessages.get(HttpStatus.FORBIDDEN),
                        responseMessages.get(HttpStatus.NOT_ACCEPTABLE),
                        responseMessages.get(HttpStatus.CONFLICT),
                        responseMessages.get(HttpStatus.INTERNAL_SERVER_ERROR)
                        )
                ).globalResponseMessage(RequestMethod.DELETE, Arrays.asList(
                        new ResponseMessage(HttpStatus.OK.value(), "Resource deleted successfully", null),
                        responseMessages.get(HttpStatus.BAD_REQUEST),
                        responseMessages.get(HttpStatus.UNAUTHORIZED),
                        responseMessages.get(HttpStatus.FORBIDDEN),
                        responseMessages.get(HttpStatus.NOT_FOUND),
                        responseMessages.get(HttpStatus.NOT_ACCEPTABLE),
                        responseMessages.get(HttpStatus.INTERNAL_SERVER_ERROR)
                )).globalResponseMessage(RequestMethod.PUT, Arrays.asList(
                        new ResponseMessage(HttpStatus.OK.value(), "Resource updated successfully", null),
                        responseMessages.get(HttpStatus.BAD_REQUEST),
                        responseMessages.get(HttpStatus.UNAUTHORIZED),
                        responseMessages.get(HttpStatus.FORBIDDEN),
                        responseMessages.get(HttpStatus.NOT_FOUND),
                        responseMessages.get(HttpStatus.NOT_ACCEPTABLE),
                        responseMessages.get(HttpStatus.INTERNAL_SERVER_ERROR)
                ));
    }

    private ApiInfo apiInfo() throws IOException {
        URL url = Resources.getResource("swagger/cloudbreak-introduction");
        ApiInfo apiInfo = new ApiInfo(
                "Cloudbreak API",
                Resources.toString(url, Charsets.UTF_8),
                "https://www.apache.org/licenses/LICENSE-2.0.html",
                "info@sequenceiq.com",
                "Apache 2.0",
                "https://www.apache.org/licenses/LICENSE-2.0.html"
        );
        return apiInfo;
    }

    private Map<HttpStatus, ResponseMessage> getResponseMessages() {
        Map<HttpStatus, ResponseMessage> responseMessageMap = new HashMap<>();
        responseMessageMap.put(HttpStatus.CREATED, new ResponseMessage(HttpStatus.CREATED.value(), "Resource created successfully", null));
        responseMessageMap.put(HttpStatus.BAD_REQUEST, new ResponseMessage(HttpStatus.BAD_REQUEST.value(), "Resource request validation error", null));
        responseMessageMap.put(HttpStatus.UNAUTHORIZED, new ResponseMessage(HttpStatus.UNAUTHORIZED.value(), "Unauthorized. Cannot access resource", null));
        responseMessageMap.put(HttpStatus.FORBIDDEN, new ResponseMessage(HttpStatus.FORBIDDEN.value(), "Forbidden. Cannot access resource", null));
        responseMessageMap.put(HttpStatus.NOT_FOUND, new ResponseMessage(HttpStatus.NOT_FOUND.value(), "Resource not found", null));
        responseMessageMap.put(HttpStatus.NOT_ACCEPTABLE, new ResponseMessage(HttpStatus.NOT_ACCEPTABLE.value(), "Media type is not acceptable", null));
        responseMessageMap.put(HttpStatus.CONFLICT, new ResponseMessage(HttpStatus.CONFLICT.value(), "Resource updated successfully", null));
        responseMessageMap.put(HttpStatus.INTERNAL_SERVER_ERROR, new ResponseMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error", null));
        return responseMessageMap;
    }
}
