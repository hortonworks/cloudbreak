package com.sequenceiq.cloudbreak.logger;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.logger.MdcContext.Builder;

@Component
public class LogContextService {

    public void buildMDCParams(Object target, String[] paramNames, Object[] args) {
        Builder builder = MdcContext.builder();
        String resourceType = null;
        for (int i = 0; i < paramNames.length; i++) {
            String paramName = paramNames[i].toLowerCase(Locale.ROOT);
            Object paramValue = args[i];
            String paramString = paramValue != null ? paramValue.toString() : "";
            if (paramName.contains("name")) {
                builder.resourceName(paramString);
            } else if (paramName.contains("request")) {
                buildMDCFromRequestParam(builder, paramValue);
            } else if (paramName.contains("environmentcrn")) {
                builder.environmentCrn(paramString);
            } else if (!paramName.contains("initiatorusercrn") && paramName.contains("crn")) {
                builder.resourceCrn(paramString);
                Crn crn = Crn.fromString(paramString);
                if (crn != null) {
                    resourceType = crn.getResourceType().getName();
                }
            }
        }
        if (resourceType == null) {
            String controllerClassName = target.getClass().getSimpleName();
            resourceType = controllerClassName.substring(0, controllerClassName.indexOf("Controller"));
            builder.resourceType(resourceType.toUpperCase(Locale.ROOT));
            builder.buildMdc();
        }
    }

    private void buildMDCFromRequestParam(Builder builder, Object paramValue) {
        String environmentCrnFieldValue = MDCBuilder.getFieldValue(paramValue, LoggerContextKey.ENVIRONMENT_CRN.toString());
        if (StringUtils.isNotBlank(environmentCrnFieldValue)) {
            builder.environmentCrn(environmentCrnFieldValue);
        }
        String resourceCrnFieldValue = MDCBuilder.getFieldValues(paramValue, LoggerContextKey.CRN.toString(), LoggerContextKey.RESOURCE_CRN.toString());
        if (StringUtils.isNotBlank(resourceCrnFieldValue)) {
            builder.resourceCrn(resourceCrnFieldValue);
        }
        String nameFieldValue = MDCBuilder.getFieldValue(paramValue, LoggerContextKey.NAME.toString());
        if (StringUtils.isNotBlank(nameFieldValue)) {
            builder.resourceName(nameFieldValue);
        }
    }
}
