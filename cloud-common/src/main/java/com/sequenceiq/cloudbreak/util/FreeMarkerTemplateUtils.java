package com.sequenceiq.cloudbreak.util;

import java.io.IOException;
import java.io.StringWriter;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.json.JsonHelper;

import freemarker.template.Template;
import freemarker.template.TemplateException;

@Service
public class FreeMarkerTemplateUtils {

    @Inject
    private JsonHelper jsonHelper;

    private FreeMarkerTemplateUtils() { }

    /**
     * Process the specified FreeMarker template with the given model and write
     * the result to the given Writer.
     * <p>When using this method to prepare a text for a mail to be sent with Spring's
     * mail support, consider wrapping IO/TemplateException in MailPreparationException.
     * @param model the model object, typically a Map that contains model names
     * as keys and model objects as values
     * @return the result as String
     * @throws IOException if the template wasn't found or couldn't be read
     * @throws freemarker.template.TemplateException if rendering failed
     */
    public String processTemplateIntoString(Template template, Object model)
            throws IOException, TemplateException {
        StringWriter result = new StringWriter();
        template.process(model, result);
        return result.toString();
    }

    public JsonNode convertStringTemplateToJson(String armTemplate) {
        return jsonHelper.createJsonFromString(armTemplate);
    }

    public String getFieldValue(String armTemplate, String fieldName) {
        JsonNode node = convertStringTemplateToJson(armTemplate);
        JsonNode foundNode = node.findValue(fieldName);
        if (foundNode.isValueNode()) {
            return foundNode.asText();
        }
        return null;
    }

}
