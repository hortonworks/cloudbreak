package com.sequenceiq.cloudbreak.logger;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MdcContext.Builder;

@Component
public class LogContextService {

    public void buildMDCParams(Object target, String[] paramNames, Object[] args) {
        Builder builder = MdcContext.builder();
        for (int i = 0; i < paramNames.length; i++) {
            String paramName = paramNames[i].toLowerCase();
            Object paramValue = args[i];
            String paramString = paramValue != null ? paramValue.toString() : "";
            if (paramName.contains("name") || paramName.contains("resourceName")) {
                builder.resourceName(paramString);
            } else if (paramName.contains("request")) {
                builder.environmentCrn(MDCBuilder.getFieldValue(paramValue, LoggerContextKey.ENVIRONMENT_CRN.toString()))
                        .resourceCrn(MDCBuilder.getFieldValues(paramValue, LoggerContextKey.CRN.toString(), LoggerContextKey.RESOURCE_CRN.toString()))
                        .resourceName(MDCBuilder.getFieldValue(paramValue, LoggerContextKey.NAME.toString()));
            } else if (paramName.contains("environmentcrn")) {
                builder.environmentCrn(paramString);
            } else if (paramName.contains("crn") || paramName.contains("resourceCrn")) {
                builder.resourceCrn(paramString);
            }
        }
        String controllerClassName = target.getClass().getSimpleName();
        String resourceType = controllerClassName.substring(0, controllerClassName.indexOf("Controller"));
        builder.resourceType(resourceType.toUpperCase());
        builder.buildMdc();
    }
}
