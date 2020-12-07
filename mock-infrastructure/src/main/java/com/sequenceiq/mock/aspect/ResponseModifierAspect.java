package com.sequenceiq.mock.aspect;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
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

    @Inject
    private ResponseModifierService responseModifierService;

    @Pointcut("execution(public * com.sequenceiq.mock..*Controller.*(..)) ")
    public void allControllers() {
    }

    @Around("allControllers()")
    public Object proceedOnRepositorySave(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        RequestMapping annotation = proceedingJoinPoint.getTarget().getClass().getAnnotation(RequestMapping.class);
        StringBuilder sb = new StringBuilder();
        appendValueOrPathIfNotBlank(annotation, sb);
        Method method = ((MethodSignature) proceedingJoinPoint.getSignature()).getMethod();
        appendValueOrPathIfNotBlank(method.getAnnotation(RequestMapping.class), sb);
        appendValueOrPathIfNotBlank(method.getAnnotation(GetMapping.class), sb);
        appendValueOrPathIfNotBlank(method.getAnnotation(PostMapping.class), sb);
        appendValueOrPathIfNotBlank(method.getAnnotation(PutMapping.class), sb);
        appendValueOrPathIfNotBlank(method.getAnnotation(DeleteMapping.class), sb);
        String resolvedPath = resolvePath(method, proceedingJoinPoint.getArgs(), sb.toString());
        return responseModifierService.evaluateResponse(resolvedPath, method.getReturnType(), proceedingJoinPoint::proceed);
    }

    private String resolvePath(Method method, Object[] args, String path) {
        String resolved = path;
        for (int i = 0; i < method.getParameters().length; i++) {
            Parameter parameter = method.getParameters()[i];
            PathVariable annotation = parameter.getAnnotation(PathVariable.class);
            if (annotation != null) {
                resolved = resolved.replace("{" + annotation.value() + "}", args[i].toString());
            }
        }
        return resolved;
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
