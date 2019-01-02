package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers.RestUrlParser.RESOURCE_EVENT;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers.RestUrlParser.RESOURCE_ID;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers.RestUrlParser.RESOURCE_NAME;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers.RestUrlParser.RESOURCE_TYPE;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers.RestUrlParser.WORKSPACE_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.UriInfo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.WorkspaceV4Endpoint;
import com.sequenceiq.cloudbreak.controller.EndpointConfig;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RestUrlParserConfig.class)
public class RestUrlParserTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestUrlParserTest.class);

    @Mock
    private ContainerRequestContext containerRequestContext;

    @Mock
    private UriInfo uriInfo;

    @Autowired
    private List<RestUrlParser> restUrlParsers;

    private String[] excludes = {"v3/utils"};

    @Test
    public void testEventUrlParser() {
        when(containerRequestContext.getMethod()).thenReturn("DELETE");
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
        RestUrlParser eventUrlParser = new V4ExistingResourceEventRestUrlParser();

        when(uriInfo.getPath()).thenReturn("v4/2/users/johnny/evict");
        Map<String, String> params = new HashMap<>();
        eventUrlParser.fillParams(containerRequestContext, params);
        assertEquals("users", params.get(RESOURCE_TYPE));
        assertEquals("evict", params.get(RESOURCE_EVENT));

        when(uriInfo.getPath()).thenReturn("v4/2/users/evict");
        params = new HashMap<>();
        eventUrlParser.fillParams(containerRequestContext, params);
        assertEquals(0, params.keySet().size());

        when(uriInfo.getPath()).thenReturn("v4/users/johnny/evict");
        params = new HashMap<>();
        eventUrlParser.fillParams(containerRequestContext, params);
        assertEquals(0, params.keySet().size());

        when(uriInfo.getPath()).thenReturn("v4/2/users/johnny/12");
        params = new HashMap<>();
        eventUrlParser.fillParams(containerRequestContext, params);
        assertEquals(0, params.keySet().size());
    }

    @Test
    public void testIfRestUrlParserMissing() {
        List<Class<?>> controllers = Whitebox.getInternalState(EndpointConfig.class, "CONTROLLERS");
        for (Class<?> controller : controllers) {
            Optional<Class<?>> endpointClass = Arrays.stream(controller.getInterfaces())
                    .filter(interf -> interf.getSimpleName().contains("Endpoint"))
                    .findFirst();
            if (endpointClass.isPresent()) {
                String rootPath = checkAndReturnPath(endpointClass);
                if (Arrays.stream(excludes).parallel().noneMatch(rootPath::contains)) {
                    for (Method method : endpointClass.get().getMethods()) {
                        String methodPath = getMethodPath(rootPath, method);
                        String requestMethod = getRequestMethod(method);
                        if (requestMethod != null) {
                            List<RestUrlParser> matchedParsers = new ArrayList<>();
                            Map<String, String> params = new HashMap<>();
                            for (RestUrlParser restUrlParser : restUrlParsers) {
                                setupMocks(methodPath, requestMethod);
                                if (restUrlParser.fillParams(containerRequestContext, params)) {
                                    matchedParsers.add(restUrlParser);
                                }
                            }
                            LOGGER.info("Matched URL: " + methodPath + "\nslurped params: " + params);
                            if (matchedParsers.isEmpty()) {
                                fail("Can not find RestUrlParser for this endpoint: " + endpointClass.get().getSimpleName()
                                        + "\nmethod: " + method.getName()
                                        + "\nurl: " + methodPath);
                            } else {
                                checkIfMoreThanOneParserDetected(methodPath, matchedParsers);
                                checkWorkspaceId(methodPath, params);
                                checkResourceIdOrName(methodPath, params);
                                checkIfResourceTypeNull(matchedParsers, params);
                            }
                        }
                    }
                }
            }
        }
    }

    private void setupMocks(String methodPath, String requestMethod) {
        when(containerRequestContext.getMethod()).thenReturn(requestMethod);
        when(uriInfo.getPath()).thenReturn(methodPath.substring(1));
        when(containerRequestContext.getUriInfo()).thenReturn(uriInfo);
    }

    private void checkIfResourceTypeNull(List<RestUrlParser> matchedParsers, Map<String, String> params) {
        if (params.get(RESOURCE_TYPE) == null) {
            fail("parser didn't get resource type: " + matchedParsers.get(0).getClass().getSimpleName());
        }
    }

    private void checkResourceIdOrName(String methodPath, Map<String, String> params) {
        if (methodPath.contains("2222")) {
            assertEquals("params should contain resource Id",
                    "2222", params.get(RESOURCE_ID));
        } else if (methodPath.contains("testresource")) {
            assertEquals("params should contain resource name",
                    "testresource", params.get(RESOURCE_NAME));
        }
    }

    private void checkWorkspaceId(String methodPath, Map<String, String> params) {
        if (methodPath.contains("1111")) {
            assertEquals("params should contain workspace id",
                    "1111", params.get(WORKSPACE_ID));
        }
    }

    private void checkIfMoreThanOneParserDetected(String methodPath, List<RestUrlParser> matchedParsers) {
        if (matchedParsers.size() > 1) {
            fail("More than one parser match for this URL: " + methodPath
                    + "\nParsers: " + matchedParsers.stream()
                    .map(restUrlParser -> restUrlParser.getClass().getSimpleName())
                    .collect(Collectors.joining(", ")));
        } else {
            LOGGER.info("Parser: {}\n", matchedParsers.get(0).getClass().getSimpleName());
        }
    }

    private String getMethodPath(String rootPath, Method method) {
        Path methodPathAnnotation = AnnotationUtils.findAnnotation(method, Path.class);
        String subpath = methodPathAnnotation == null ? "" : methodPathAnnotation.value();
        if (!subpath.isEmpty() && !subpath.startsWith("/")) {
            subpath = '/' + subpath;
        }
        String methodPath = rootPath + subpath;
        methodPath = replacePathParamsWithExamples(method, methodPath);
        return methodPath;
    }

    private String checkAndReturnPath(Optional<Class<?>> endpointClass) {
        Path controllerPathAnnotation = AnnotationUtils.findAnnotation(endpointClass.get(), Path.class);
        if (controllerPathAnnotation == null) {
            fail("Can not find Path annotation for this controller: " + endpointClass.get().getSimpleName());
        }
        return controllerPathAnnotation.value();
    }

    private String getRequestMethod(Method method) {
        String requestMethod = null;
        if (AnnotationUtils.findAnnotation(method, PUT.class) != null) {
            requestMethod = "PUT";
        }
        if (AnnotationUtils.findAnnotation(method, POST.class) != null) {
            requestMethod = "POST";
        }
        if (AnnotationUtils.findAnnotation(method, DELETE.class) != null) {
            requestMethod = "DELETE";
        }
        return requestMethod;
    }

    private String replacePathParamsWithExamples(Method method, String methodPath) {
        for (Parameter parameter : method.getParameters()) {
            PathParam pathParam = AnnotationUtils.findAnnotation(parameter, PathParam.class);
            if (pathParam != null) {
                String pathParamValue = pathParam.value();
                Class<?> parameterType = parameter.getType();
                if (Long.class.equals(parameterType)) {
                    methodPath = "workspaceId".equals(pathParamValue)
                            ? methodPath.replace('{' + pathParamValue + '}', "1111")
                            : methodPath.replace('{' + pathParamValue + '}', "2222");
                } else {
                    methodPath = method.getDeclaringClass().equals(WorkspaceV4Endpoint.class)
                            ? methodPath.replace('{' + pathParamValue + '}', "some-stupid-resource-name-34")
                            : methodPath.replace('{' + pathParamValue + '}', "Some_Stupid:Resource Name:");
                }
            }
        }
        return methodPath;
    }

}