package com.sequenceiq.mock.aspect;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Set;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sequenceiq.mock.service.ResponseModifierService;

@Component
@Aspect
public class ResponseModifierAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseModifierAspect.class);

    @Inject
    private ResponseModifierService responseModifierService;

    @Pointcut("execution(public * com.sequenceiq.mock..*Controller.*(..)) ")
    public void allControllers() {
    }

    @Around("allControllers()")
    public Object proceedOnRepositorySave(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        RequestMapping annotation = AnnotationUtils.findAnnotation(proceedingJoinPoint.getTarget().getClass(), RequestMapping.class);
        StringBuilder sb = new StringBuilder();
        appendValueOrPathIfNotBlank(annotation, sb);
        Method method = ((MethodSignature) proceedingJoinPoint.getSignature()).getMethod();
        appendValueOrPathIfNotBlank(AnnotationUtils.findAnnotation(method, RequestMapping.class), sb);
        appendValueOrPathIfNotBlank(AnnotationUtils.findAnnotation(method, GetMapping.class), sb);
        appendValueOrPathIfNotBlank(AnnotationUtils.findAnnotation(method, PostMapping.class), sb);
        appendValueOrPathIfNotBlank(AnnotationUtils.findAnnotation(method, PutMapping.class), sb);
        appendValueOrPathIfNotBlank(AnnotationUtils.findAnnotation(method, DeleteMapping.class), sb);
        LOGGER.debug("Called method: {}", method);
        String resolvedPath = resolvePath(method, proceedingJoinPoint.getArgs(), sb.toString());
        String mockUuid = getArgByName(method, proceedingJoinPoint.getArgs(), Set.of("mock_uuid", "mockuuid", "mockUuid"));
        responseModifierService.handleProfiles(mockUuid, resolvedPath);
        return responseModifierService.evaluateResponse(resolvedPath, method.getReturnType(), proceedingJoinPoint::proceed);
    }

    private String resolvePath(Method method, Object[] args, String path) {
        String resolved = path;
        Method m = searchInterfaceMethod(method);
        for (int i = 0; i < m.getParameters().length; i++) {
            Parameter parameter = m.getParameters()[i];
            PathVariable annotation = parameter.getAnnotation(PathVariable.class);
            if (annotation != null) {
                resolved = resolved.replace("{" + annotation.value() + "}", args[i].toString());
            }
        }
        return resolved;
    }

    private Method searchInterfaceMethod(Method method) {
        return Arrays.stream(method.getDeclaringClass().getInterfaces())
                .filter(c -> Arrays.stream(c.getMethods()).anyMatch(m -> m.getName().equals(method.getName())))
                .findFirst()
                .map(c -> Arrays.stream(c.getMethods()).filter(m -> m.getName().equals(method.getName())).findFirst().orElse(null))
                .orElse(method);
    }

    private String getArgByName(Method method, Object[] args, Set<String> argNames) {
        Method m = searchInterfaceMethod(method);
        for (int i = 0; i < m.getParameters().length; i++) {
            Parameter parameter = m.getParameters()[i];
            PathVariable annotation = parameter.getAnnotation(PathVariable.class);
            if (annotation != null && (argNames.contains(annotation.name()) || argNames.contains(annotation.value()))) {
                Object arg = args[i];
                return arg == null ? null : arg.toString();
            }
        }
        return null;
    }

    private void appendValueOrPathIfNotBlank(RequestMapping annotation, StringBuilder sb) {
        if (annotation != null) {
            if (annotation.value().length > 0 && !StringUtils.isEmpty(annotation.value())) {
                String method = "";
                if (annotation.method().length > 0) {
                    method = annotation.method()[0].name().toLowerCase() + "_";
                }
                sb.insert(0, method).append(annotation.value()[0]);
            }
        }
    }

    private void appendValueOrPathIfNotBlank(GetMapping annotation, StringBuilder sb) {
        if (annotation != null) {
            if (annotation.value().length > 0 && !StringUtils.isEmpty(annotation.value())) {
                sb.insert(0, "get_").append(annotation.value()[0]);
            }
        }
    }

    private void appendValueOrPathIfNotBlank(PostMapping annotation, StringBuilder sb) {
        if (annotation != null) {
            if (annotation.value().length > 0 && !StringUtils.isEmpty(annotation.value())) {
                sb.insert(0, "post_").append(annotation.value()[0]);
            }
        }
    }

    private void appendValueOrPathIfNotBlank(PutMapping annotation, StringBuilder sb) {
        if (annotation != null) {
            if (annotation.value().length > 0 && !StringUtils.isEmpty(annotation.value())) {
                sb.insert(0, "put_").append(annotation.value()[0]);
            }
        }
    }

    private void appendValueOrPathIfNotBlank(DeleteMapping annotation, StringBuilder sb) {
        if (annotation != null) {
            if (annotation.value().length > 0 && !StringUtils.isEmpty(annotation.value())) {
                sb.insert(0, "delete_").append(annotation.value()[0]);
            }
        }
    }
}
