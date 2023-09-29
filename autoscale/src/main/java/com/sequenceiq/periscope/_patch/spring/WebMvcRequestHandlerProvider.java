// CHECKSTYLE:OFF
package springfox.documentation.spring.web.plugins;

/*
 *
 *  Copyright 2015-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  !!!!!!!!!!!!!!!!!
 *  !!!!IMPORTANT!!!! Leave this override here until springfox fixes https://github.com/springfox/springfox/issues/3462. Do not touch package name either.
 *  !!!!!!!!!!!!!!!!!
 *
 */

import static java.util.stream.Collectors.toList;
import static springfox.documentation.builders.BuilderDefaults.nullToEmptyList;
import static springfox.documentation.spi.service.contexts.Orderings.byPatternsCondition;
import static springfox.documentation.spring.web.paths.Paths.ROOT;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

import springfox.documentation.RequestHandler;
import springfox.documentation.spi.service.RequestHandlerProvider;
import springfox.documentation.spring.web.OnServletBasedWebApplication;
import springfox.documentation.spring.web.WebMvcRequestHandler;
import springfox.documentation.spring.web.readers.operation.HandlerMethodResolver;

@Primary
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Conditional(OnServletBasedWebApplication.class)
public class WebMvcRequestHandlerProvider implements RequestHandlerProvider {

    private final List<RequestMappingInfoHandlerMapping> handlerMappings;

    private final HandlerMethodResolver methodResolver;

    private final String contextPath;

    @Autowired
    public WebMvcRequestHandlerProvider(
            Optional<ServletContext> servletContext,
            HandlerMethodResolver methodResolver,
            List<RequestMappingInfoHandlerMapping> handlerMappings) {
        this.handlerMappings = handlerMappings.stream().filter(mapping -> mapping.getPatternParser() == null)
                .collect(toList());
        this.methodResolver = methodResolver;
        this.contextPath = servletContext
                .map(ServletContext::getContextPath)
                .orElse(ROOT);
    }

    @Override
    public List<RequestHandler> requestHandlers() {
        return nullToEmptyList(handlerMappings).stream()
                .filter(requestMappingInfoHandlerMapping ->
                        !("org.springframework.integration.http.inbound.IntegrationRequestMappingHandlerMapping"
                                .equals(requestMappingInfoHandlerMapping.getClass()
                                        .getName())))
                .map(toMappingEntries())
                .flatMap((entries -> StreamSupport.stream(entries.spliterator(), false)))
                .map(toRequestHandler())
                .sorted(byPatternsCondition())
                .collect(toList());
    }

    private Function<RequestMappingInfoHandlerMapping,
            Iterable<Map.Entry<RequestMappingInfo, HandlerMethod>>> toMappingEntries() {
        return input -> input.getHandlerMethods()
                .entrySet();
    }

    private Function<Map.Entry<RequestMappingInfo, HandlerMethod>, RequestHandler> toRequestHandler() {
        return input -> new WebMvcRequestHandler(
                contextPath,
                methodResolver,
                input.getKey(),
                input.getValue());
    }
}
// CHECKSTYLE:ON
