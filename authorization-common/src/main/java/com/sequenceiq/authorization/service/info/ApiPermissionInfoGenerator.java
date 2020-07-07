package com.sequenceiq.authorization.service.info;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MemberUsageScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.MethodParameterNamesScanner;
import org.reflections.scanners.MethodParameterScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceObject;
import com.sequenceiq.authorization.annotation.CustomPermissionCheck;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceObject;
import com.sequenceiq.authorization.annotation.ResourceObjectField;
import com.sequenceiq.authorization.info.model.ApiAuthorizationInfo;
import com.sequenceiq.authorization.info.model.ApiAuthorizationInfoBuilder;
import com.sequenceiq.authorization.info.model.FieldAuthorizationInfo;
import com.sequenceiq.authorization.info.model.LegacyAuthorizationInfo;
import com.sequenceiq.authorization.info.model.NewAuthorizationInfo;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.UmsRightProvider;
import com.sequenceiq.authorization.util.AuthorizationAnnotationUtils;

@Service
public class ApiPermissionInfoGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiPermissionInfoGenerator.class);

    private static final Reflections REFLECTIONS = new Reflections("com.sequenceiq",
            new FieldAnnotationsScanner(),
            new TypeAnnotationsScanner(),
            new SubTypesScanner(false),
            new MemberUsageScanner(),
            new MethodAnnotationsScanner(),
            new MethodParameterScanner(),
            new MethodParameterNamesScanner());

    @Inject
    private UmsRightProvider umsRightProvider;

    public Set<ApiAuthorizationInfo> generateApiMethodsWithRequiredPermission() {
        Set<Class<?>> authorizationResourceClasses = REFLECTIONS.getTypesAnnotatedWith(AuthorizationResource.class);
        Set<ApiAuthorizationInfo> infos = Sets.newHashSet();
        authorizationResourceClasses.stream().forEach(authzClass -> infos.addAll(getMethodInfosForClass(authzClass)));
        return infos;
    }

    private Set<ApiAuthorizationInfo> getMethodInfosForClass(Class<?> authzClass) {
        Set<ApiAuthorizationInfo> pathPermissions = Sets.newHashSet();
        Arrays.stream(authzClass.getInterfaces())
                .filter(controllerInterface -> controllerInterface.isAnnotationPresent(Path.class))
                .forEach(controllerInterface -> Arrays.stream(controllerInterface.getMethods())
                        .filter(method -> method.isAnnotationPresent(Path.class))
                        .forEach(method -> {
                            String apiPath = controllerInterface.getAnnotation(Path.class).value();
                            String methodPath = apiPath + getMethodPath(method);
                            String httpMethod = getMethodType(method);
                            pathPermissions.add(getAuthorizationInfo(methodPath, httpMethod, controllerInterface, method));
                        }));
        return pathPermissions;
    }

    private String getMethodPath(Method method) {
        String methodPath = method.getAnnotation(Path.class).value();
        if (!methodPath.startsWith("/") && !methodPath.isEmpty()) {
            methodPath = "/" + methodPath;
        }
        return methodPath;
    }

    private ApiAuthorizationInfo getAuthorizationInfo(String methodPath, String httpMethod, Class apiInterface, Method method) {
        AtomicReference<ApiAuthorizationInfo> result = new AtomicReference<>();
        Set<Class> interfaceSubTypes = REFLECTIONS.getSubTypesOf(apiInterface);
        Arrays.stream(interfaceSubTypes.iterator().next().getMethods())
                .filter(controllerMethod -> StringUtils.equals(controllerMethod.getName(), method.getName()))
                .forEach(controllerMethod -> result.set(getAuthorizationInfoFromAnnotations(methodPath, httpMethod, controllerMethod)));
        return result.get();
    }

    private static String getMethodType(Method method) {
        Set<String> httpAnnotations = Lists.newArrayList(GET.class, DELETE.class, PUT.class, POST.class, HEAD.class, OPTIONS.class, PATCH.class)
                .stream()
                .filter(httpAnnotation -> method.isAnnotationPresent(httpAnnotation))
                .map(httpAnnotation -> httpAnnotation.getSimpleName())
                .collect(Collectors.toSet());
        return Joiner.on(" ").join(httpAnnotations);
    }

    private ApiAuthorizationInfo getAuthorizationInfoFromAnnotations(String methodPath, String httpMethod, Method method) {
        NewAuthorizationInfo newAuthorizationInfo = new NewAuthorizationInfo();
        LegacyAuthorizationInfo legacyAuthorizationInfo = new LegacyAuthorizationInfo();
        String message = null;
        getInfoFromRegularAnnotation(method, newAuthorizationInfo, legacyAuthorizationInfo);
        if (method.isAnnotationPresent(CheckPermissionByResourceObject.class)) {
            getInfoFromResourceObjectAnnotation(method, newAuthorizationInfo, legacyAuthorizationInfo);
            message = "Authorization happening on specific field(s) of request object, this is happening only in case of new authorization.";
        }
        if (method.isAnnotationPresent(DisableCheckPermissions.class)) {
            message = "Permission check disabled.";
        }
        if (method.isAnnotationPresent(CustomPermissionCheck.class)) {
            message = "Custom permission check implemented, please ask developers for more information.";
        }
        if (method.isAnnotationPresent(InternalOnly.class)) {
            message = "This API can be called by internal actor only.";
        }
        return ApiAuthorizationInfoBuilder.anApiAuthorizationInfo()
                .withHttpMethod(httpMethod)
                .withPath(methodPath)
                .withMessage(message)
                .withLegacyAuthorization(legacyAuthorizationInfo)
                .withNewAuthorization(newAuthorizationInfo)
                .build();
    }

    private void getInfoFromResourceObjectAnnotation(Method method, NewAuthorizationInfo newAuthorizationInfo, LegacyAuthorizationInfo legacyAuthorizationInfo) {
        Set<FieldAuthorizationInfo> fieldPermissions = Sets.newHashSet();
        Arrays.stream(method.getParameters())
                .filter(parameter -> parameter.isAnnotationPresent(ResourceObject.class))
                .forEach(parameter -> {
                    Class resourceObjectType = parameter.getType();
                    getAllFields(Lists.newArrayList(), resourceObjectType).stream()
                            .filter(field -> field.isAnnotationPresent(ResourceObjectField.class))
                            .forEach(field -> {
                                ResourceObjectField resourceObjectField = field.getAnnotation(ResourceObjectField.class);
                                FieldAuthorizationInfo fieldAuthorizationInfo = new FieldAuthorizationInfo(field.getName(),
                                        umsRightProvider.getNewRight(resourceObjectField.action()));
                                fieldPermissions.add(fieldAuthorizationInfo);
                                legacyAuthorizationInfo.getPermissionNeeded().add(umsRightProvider.getLegacyRight(resourceObjectField.action()));
                            });
                });
        newAuthorizationInfo.getPermissionsNeededForRequestObject().addAll(fieldPermissions);
    }

    private void getInfoFromRegularAnnotation(Method method, NewAuthorizationInfo newAuthorizationInfo, LegacyAuthorizationInfo legacyAuthorizationInfo) {
        AuthorizationAnnotationUtils.getPossibleMethodAnnotations().stream()
                .filter(annotation -> method.isAnnotationPresent(annotation))
                .filter(annotation -> Arrays.stream(method.getAnnotation(annotation).annotationType().getMethods())
                        .filter(annotationMethod -> StringUtils.equals(annotationMethod.getName(), "action"))
                        .findFirst()
                        .isPresent())
                .forEach(annotation -> {
                    try {
                        Annotation actualAnnotation = method.getAnnotation(annotation);
                        Method actionMethod = actualAnnotation.annotationType().getMethod("action");
                        AuthorizationResourceAction action = (AuthorizationResourceAction) actionMethod.invoke(actualAnnotation);
                        newAuthorizationInfo.getPermissionsNeededForApi().add(umsRightProvider.getNewRight(action));
                        legacyAuthorizationInfo.getPermissionNeeded().add(umsRightProvider.getLegacyRight(action));
                    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                        LOGGER.error("Error: ", e);
                    }
                });
    }

    public static List<Field> getAllFields(List<Field> fields, Class<?> type) {
        fields.addAll(Arrays.asList(type.getDeclaredFields()));
        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }
        return fields;
    }
}
