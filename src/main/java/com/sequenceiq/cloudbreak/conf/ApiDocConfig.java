package com.sequenceiq.cloudbreak.conf;

import com.mangofactory.swagger.configuration.SpringSwaggerConfig;
import com.mangofactory.swagger.models.dto.ApiInfo;
import com.mangofactory.swagger.plugin.EnableSwagger;
import com.mangofactory.swagger.plugin.SwaggerSpringMvcPlugin;
import com.sequenceiq.cloudbreak.domain.CbUser;
import org.ajar.swaggermvcui.SwaggerSpringMvcUi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableSwagger
@EnableWebMvc
public class ApiDocConfig extends WebMvcConfigurerAdapter {

    @Autowired
    private SpringSwaggerConfig springSwaggerConfig;

    @Bean
    public SwaggerSpringMvcPlugin customImplementation() {
        return new SwaggerSpringMvcPlugin(this.springSwaggerConfig)
                .apiInfo(apiInfo())
                .includePatterns(".*stack.*", ".*cluster.*", ".*blueprint.*", ".*template.*",
                        ".*credential.*", ".*recipe.*", ".*usage.*", ".*event.*")
                .ignoredParameterTypes(CbUser.class, ModelMap.class, View.class, ModelAndView.class);
    }

    private ApiInfo apiInfo() {
        ApiInfo apiInfo = new ApiInfo(
                "Cloudbreak API",
                "Cloud agnostic Hadoop as a Service API",
                "https://www.apache.org/licenses/LICENSE-2.0.html",
                "info@sequenceiq.com",
                "Apache 2.0",
                "https://www.apache.org/licenses/LICENSE-2.0.html"
        );
        return apiInfo;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(new String[]{
                "css/**",
                "images/**",
                "lib/**",
                "swagger-ui.js",
                "o2c.html",
                "favicon.ico",
                "docs.html"})
                .addResourceLocations(new String[]{SwaggerSpringMvcUi.WEB_JAR_RESOURCE_LOCATION + "css/",
                        SwaggerSpringMvcUi.WEB_JAR_RESOURCE_LOCATION + "images/",
                        SwaggerSpringMvcUi.WEB_JAR_RESOURCE_LOCATION + "lib/",
                        SwaggerSpringMvcUi.WEB_JAR_RESOURCE_LOCATION + "swagger-ui.js",
                        SwaggerSpringMvcUi.WEB_JAR_RESOURCE_LOCATION + "o2c.html",
                        "classpath:META-INF/favicon.ico",
                        "classpath:META-INF/docs.html"})
                .setCachePeriod(0);
    }
}