package com.sequenceiq.it.cloudbreak.performance;

public class BasicStatisticFormatter implements KeyMeasurementFormatter {
    @Override
    public String header() {
        return "      <table style=\"border: 1px solid black;\">\n"
                + "        <caption>Key performance indicators</caption>\n"
                + "          <tr style=\"border: 1px solid black;\">\n"
                + "            <th style=\"border: 1px solid black;\">Action</th>\n"
                + "            <th style=\"border: 1px solid black;\">Average (ms)</th>\n"
                + "            <th style=\"border: 1px solid black;\">Deviation (ms)</th>\n"
                + "          </tr>";
    }

    @Override
    public String element(KeyMeasurement p) {
        return "        <tr style=\"border: 1px solid black;\">\n"
                + "          <td style=\"border: 1px solid black;\">" + ((BasicStatistic) p).getAction() + "</td>\n"
                + "          <td style=\"border: 1px solid black;\">" + ((BasicStatistic) p).getAverage() + "</td>\n"
                + "          <td style=\"border: 1px solid black;\">" + ((BasicStatistic) p).getDeviation() + "</td>\n"
                + "        </tr>";
    }

    @Override
    public String foot() {
        return "      </table>";
    }
}
