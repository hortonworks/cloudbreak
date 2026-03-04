package com.sequenceiq.cloudbreak.cluster.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HostnameTransformer {

    private static final Pattern SHORT_HOSTNAME_PATTERN = Pattern.compile("(.*?)(\\d+)$");

    private static final int FALLBACK_INDEX = -1;

    private HostnameTransformer() {

    }

    /**
     * @param fqdns list of Fully Qualified Domain Names
     * @return patterns parsed from fqdns, e.g. compute0, master0, worker[0-1,3-4]
     */
    public static List<String> getHostnamePatterns(List<String> fqdns) {
        Map<String, List<Integer>> groups = new LinkedHashMap<>();

        for (String fqdn : fqdns) {
            String hostname = fqdn.split("\\.")[0];
            String[] hostNameParts = hostname.split("-");
            String shortHostName = hostNameParts[hostNameParts.length - 1];
            Matcher matcher = SHORT_HOSTNAME_PATTERN.matcher(shortHostName);
            if (matcher.find()) {
                String hostgroup = matcher.group(1);
                int index = Integer.parseInt(matcher.group(2));
                groups.computeIfAbsent(hostgroup, k -> new ArrayList<>()).add(index);
            } else {
                groups.computeIfAbsent(hostname, k -> new ArrayList<>()).add(FALLBACK_INDEX);
            }
        }

        List<String> patterns = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> entry : groups.entrySet()) {
            patterns.add(entry.getKey() + formatRanges(entry.getValue()));
        }

        return patterns.stream().sorted().toList();
    }

    /**
     * Formats a list of numbers as a string of ranges
     * @param numbers e.g. [0, 1, 3, 4]
     * @return e.g. "0-1,3-4"
     */
    private static String formatRanges(List<Integer> numbers) {
        if (numbers.isEmpty() || numbers.contains(FALLBACK_INDEX)) {
            return "";
        }
        if (numbers.size() == 1) {
            return String.valueOf(numbers.getFirst());
        }

        List<String> ranges = new ArrayList<>();
        int rangeStart = numbers.getFirst();
        int prev = numbers.getFirst();

        for (int j = 1; j < numbers.size(); j++) {
            int current = numbers.get(j);

            if (current != prev + 1) {
                ranges.add(formatRange(rangeStart, prev));
                rangeStart = current;
            }

            prev = current;
        }
        ranges.add(formatRange(rangeStart, numbers.getLast()));

        return '[' + String.join(",", ranges) + ']';
    }

    private static String formatRange(int start, int end) {
        return (start == end) ? String.valueOf(start) : start + "-" + end;
    }
}
