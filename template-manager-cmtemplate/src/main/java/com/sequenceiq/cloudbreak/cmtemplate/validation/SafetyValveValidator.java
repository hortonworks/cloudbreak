package com.sequenceiq.cloudbreak.cmtemplate.validation;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.sequenceiq.cloudbreak.template.model.SafetyValve;

@Component
public class SafetyValveValidator {
    private static final List<String> NON_XML_FORMAT_SAFETY_VALVE = List.of("hue_service_safety_valve",
            "spark-conf/spark-defaults.conf_client_config_safety_valve",
            "impala_cmd_args_safety_valve",
            "hdfs_client_env_safety_valve",
            "mapreduce_client_env_safety_valve",
            "spark3-conf/spark-defaults.conf_client_config_safety_valve",
            "REGIONSERVER_role_env_safety_valve",
            "hue_server_hue_safety_valve");

    private final XmlMapper xmlMapper = new XmlMapper();

    public void validate(SafetyValve safetyValve) {
        if (NON_XML_FORMAT_SAFETY_VALVE.contains(safetyValve.getName())) {
            return;
        }
        if (safetyValve.getRawValue() == null) {
            throw new IllegalArgumentException(String.format("Safety valve value is not present for {serviceType: %s, roleType: %s, safety valve: %s}",
                    safetyValve.getServiceType(), safetyValve.getRoleType(), safetyValve.getName()));
        }
        String wrappedValue = getWrappedValue(safetyValve.getRawValue());
        try {
            xmlMapper.readValue(wrappedValue, SafetyValve.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(String.format("Format is invalid for {serviceType: %s, roleType: %s, safety valve: %s, raw value: %s}",
                    safetyValve.getServiceType(), safetyValve.getRoleType(), safetyValve.getName(), safetyValve.getRawValue()));
        }
    }

    private String getWrappedValue(String value) {
        return new StringBuilder(value).insert(0, "<SafetyValve><properties>").append("</properties></SafetyValve>").toString();
    }

}
