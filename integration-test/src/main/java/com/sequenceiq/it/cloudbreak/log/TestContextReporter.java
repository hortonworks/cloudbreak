package com.sequenceiq.it.cloudbreak.log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.web.util.HtmlUtils;

class TestContextReporter {
    private List<Step> steps;

    TestContextReporter() {
        steps = new ArrayList<>();
    }

    public void addStep(String step, String description) {
        steps.add(new Step(step, description));
    }

    public void addStep(String step, String description, String json) {
        steps.add(new Step(step, description, json));
    }

    public String print() {
        if (steps.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();


        String previous = steps.get(steps.size() - 1).getStep();
        int rowspan = 1;
        String previousStartTag = "";
        for (int i = steps.size() - 1; i >= 0; i--) {
            Step currentStep = steps.get(i);
            if (!previous.equals(currentStep.getStep())) {
                if (rowspan != 1) {
                    result.insert(0, "<tr><th rowspan=\" " + rowspan + "\">" + previous + "</th>");
                } else {
                    result.insert(0, "<tr><th>" + previous + "</th>");
                }
                result.insert(0, "<td>" + currentStep.getDescription() + jsonHtml(currentStep.getJson()) + "</td></tr>");
                rowspan = 1;
                previous = currentStep.getStep();
                previousStartTag = "";
            } else {
                result.insert(0, "<td>" + currentStep.getDescription() + jsonHtml(currentStep.getJson()) + "</td></tr>" + previousStartTag);
                rowspan++;
                previousStartTag = "<tr>";
            }
            previous = currentStep.getStep();

        }

        if (rowspan != 1) {
            result.insert(0, "<tr><th rowspan=\" " + rowspan + "\">" + previous + "</th>");
        } else {
            result.insert(0, "<tr><td>" + previous + "</td>");
        }

        result.insert(0, "<table style=\"border: 1px solid black; border-collapse: collapse;\">"
                + "<caption>Test steps</caption>"
                + "<tr style=\"border: 1px solid black;\">"
                + "<th style=\"border: 1px solid black;\">Step</th>"
                + "<th style=\"border: 1px solid black;\">Details</th>"
                + "</tr><tr>");
        result.append("</table>");

        return result.toString();
    }

    private String jsonHtml(String json) {
        if (json == null || "".equals(json)) {
            return "";
        }
        String uniqueID = UUID.randomUUID().toString();
        return "<input type=\"text\" value=\"" + HtmlUtils.htmlEscape(json) + "\" id=\"" + uniqueID + "\" size=80 disabled=true>"
                + "<button onclick=\"copyToClipboard('" + uniqueID + "')\">Copy JSON</button>";
    }
}
