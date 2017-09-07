package com.sequenceiq.cloudbreak.shell.support;

import static java.util.Collections.singletonList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.shell.support.table.Table;
import org.springframework.shell.support.table.TableHeader;
import org.springframework.stereotype.Component;

/**
 * Utility class used to render tables.
 */
@Component
public class TableRenderer {

    public static final int THREE = 3;

    /**
     * Renders a 2 columns wide table with the given headers and rows. If headers are provided it should match with the
     * number of columns.
     *
     * @param rows                  rows of the table
     * @param headers               headers of the table
     * @return the formatted table
     */
    public  <E> String renderSingleMapWithSortedColumn(Map<E, String> rows, String... headers) {
        return renderMultiValueMap(convert(rows), true, headers);
    }

    /**
     * Renders a 2 columns wide table with the given headers and rows. If headers are provided it should match with the
     * number of columns.
     *
     * @param rows                  rows of the table, each value will be added as a new row with the same key
     * @param sortByFirstColumn     sortByFirstColumn rows by the first column
     * @param headers               headers of the table
     * @return formatted table
     */
    public String renderMultiValueMap(Map<String, List<String>> rows, boolean sortByFirstColumn, String... headers) {
        Table table = createTable(headers);
        if (rows != null) {
            List<Entry<String, List<String>>> entries = new ArrayList<>(rows.entrySet());
            if (sortByFirstColumn) {
                entries.sort(new Comparator<Entry<String, List<String>>>() {
                    @Override
                    public int compare(Entry<String, List<String>> a, Entry<String, List<String>> b) {
                        if (isEmpty(a) || isEmpty(b)) {
                            return compareKeys(a, b);
                        }
                        int comp = a.getValue().get(0).compareToIgnoreCase(b.getValue().get(0));
                        return comp == 0 ? compareKeys(a, b) : comp;
                    }

                    private int compareKeys(Entry<String, List<String>> a, Entry<String, List<String>> b) {
                        return a.getKey().compareToIgnoreCase(b.getKey());
                    }

                    private boolean isEmpty(Entry<String, List<String>> input) {
                        return input.getValue() == null || input.getValue().isEmpty() || input.getValue().get(0) == null;
                    }
                });
            } else {
                entries.sort((a, b) -> a.getKey().compareToIgnoreCase(b.getKey()));
            }
            for (Entry<String, List<String>> entry : entries) {
                if (entry.getValue() != null) {
                    for (String value : entry.getValue()) {
                        table.addRow(entry.getKey(), value);
                    }
                }
            }
        }
        return format(table);
    }

    public <E> String renderObjectValueMap(Map<String, E> rows, String mainHeader) {
        Table table = new Table();
        List<String> mainHeaders = new ArrayList<>();
        if (rows != null) {
            int index = 0;
            for (Entry<String, E> entry : rows.entrySet()) {
                Object value = entry.getValue();
                if (value != null) {
                    Field[] fields = value.getClass().getDeclaredFields();
                    if (index == 0) {
                        List<String> headers = new ArrayList<>();
                        headers.add(mainHeader);
                        for (Field classField : fields) {
                            if (!classField.getName().equals(mainHeader)) {
                                headers.add(classField.getName());
                                mainHeaders.add(classField.getName());
                            }
                        }
                        table = createTable(headers.toArray(new String[headers.size()]));
                    }
                    List<String> rowValues = new ArrayList<>(fields.length + 1);
                    rowValues.add(entry.getKey());
                    for (Field classField : fields) {
                        if (mainHeaders.contains(classField.getName())) {
                            classField.setAccessible(true);
                            Object o = runGetter(classField, value);
                            if (o != null) {
                                rowValues.add(o.toString());
                            }
                        }
                    }
                    table.addRow(rowValues.toArray(new String[rowValues.size()]));
                    index++;
                }
            }
        }
        return format(table);
    }

    private Object runGetter(Field field, Object o) {
        for (Method method : o.getClass().getMethods()) {
            if ((method.getName().startsWith("get")) && (method.getName().length() == (field.getName().length() + THREE))) {
                if (method.getName().toLowerCase().endsWith(field.getName().toLowerCase())) {
                    try {
                        return method.invoke(o);
                    } catch (Exception e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    private static Table createTable(String... headers) {
        Table table = new Table();
        if (headers != null) {
            int column = 1;
            for (String header : headers) {
                table.addHeader(column++, new TableHeader(header));
            }
        }
        return table;
    }

    private static <E> Map<String, List<String>> convert(Map<E, String> map) {
        Map<String, List<String>> result = new HashMap<>();
        if (map != null) {
            for (Entry<E, String> entry : map.entrySet()) {
                if (entry.getValue() != null) {
                    result.put(entry.getKey().toString(), singletonList(entry.getValue()));
                }
            }
        }
        return result;
    }

    private static String format(Table table) {
        table.calculateColumnWidths();
        return table.toString();
    }
}
