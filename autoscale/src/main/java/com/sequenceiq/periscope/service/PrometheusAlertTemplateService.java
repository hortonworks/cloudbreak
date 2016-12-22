package com.sequenceiq.periscope.service;

import static org.springframework.ui.freemarker.FreeMarkerTemplateUtils.processTemplateIntoString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.periscope.api.model.AlertRuleDefinitionEntry;

import freemarker.template.Configuration;
import freemarker.template.Template;

@Service
public class PrometheusAlertTemplateService {

    private static final String ALERT_PATH = "alerts/prometheus/";

    private static final String FILE_EXTENSTION = ".ftl";

    private static final List<AlertRuleDefinitionEntry> ALERT_DEFINITIONS = new ArrayList<>();

    static {
        ALERT_DEFINITIONS.add(new AlertRuleDefinitionEntry("cpu_threshold_exceeded", "CPU usage"));
        ALERT_DEFINITIONS.add(new AlertRuleDefinitionEntry("memory_threshold_exceeded", "Memory usage"));
        ALERT_DEFINITIONS.add(new AlertRuleDefinitionEntry("namenode_capacity_threshold_exceeded", "HDFS usage"));
    }

    @Inject
    private Configuration freemarkerConfiguration;

    public String createAlert(String alertRuleTemplateName, String name, String threshold, int period) throws Exception {
        Map<String, String> model = new HashMap<>();
        model.put("alertName", name);
        model.put("threshold", threshold);
        model.put("period", String.valueOf(period));
        Template template = freemarkerConfiguration.getTemplate(ALERT_PATH + alertRuleTemplateName + FILE_EXTENSTION, "UTF-8");
        return processTemplateIntoString(template, model);
    }

    public List<AlertRuleDefinitionEntry> getAlertDefinitions() {
        return ALERT_DEFINITIONS;
    }
}
