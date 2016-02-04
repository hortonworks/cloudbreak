package com.sequenceiq.cloudbreak.shell.support;

import static java.util.Collections.singletonList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.shell.support.table.Table;
import org.springframework.shell.support.table.TableHeader;

/**
 * Utility class used to render tables.
 */
public final class TableRenderer {

    public static final int THREE = 3;

    private TableRenderer() {
        throw new IllegalStateException();
    }

    /**
     * Renders a 2 columns wide table with the given headers and rows. If headers are provided it should match with the
     * number of columns.
     *
     * @param rows    rows of the table
     * @param headers headers of the table
     * @return the formatted table
     */
    public static String renderSingleMap(Map<String, String> rows, String... headers) {
        return renderSingleMap(rows, false, headers);
    }

    /**
     * Renders a 2 columns wide table with the given headers and rows. If headers are provided it should match with the
     * number of columns.
     *
     * @param rows                  rows of the table
     * @param sortByFirstColumn     sortByFirstColumn rows by the first column
     * @param headers               headers of the table
     * @return the formatted table
     */
    public static String renderSingleMap(Map<String, String> rows, boolean sortByFirstColumn, String... headers) {
        return renderMultiValueMap(convert(rows), sortByFirstColumn, headers);
    }

    public static <E extends Object> String renderObjectMap(Map<String, E> rows, String... headers) {
        return renderMultiValueMap(convertObjectMap(rows), headers);
    }

    /**
     * Renders a 2 columns wide table with the given headers and rows. If headers are provided it should match with the
     * number of columns.
     *
     * @param rows    rows of the table, each value will be added as a new row with the same key
     * @param headers headers of the table
     * @return formatted table
     */
    public static String renderMultiValueMap(Map<String, List<String>> rows, String... headers) {
        return renderMultiValueMap(rows, false, headers);
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
    public static String renderMultiValueMap(Map<String, List<String>> rows, boolean sortByFirstColumn, String... headers) {
        Table table = createTable(headers);
        if (rows != null) {
            List<Map.Entry<String, List<String>>> entries = new ArrayList<>(rows.entrySet());
            if (sortByFirstColumn) {
                Collections.sort(entries, new Comparator<Map.Entry<String, List<String>>>() {
                    @Override
                    public int compare(Map.Entry<String, List<String>> a, Map.Entry<String, List<String>> b) {
                        if (isEmpty(a) || isEmpty(b)) {
                            return compareKeys(a, b);
                        }
                        int comp = a.getValue().get(0).compareToIgnoreCase(b.getValue().get(0));
                        return comp == 0 ? compareKeys(a, b) : comp;
                    }

                    private int compareKeys(Map.Entry<String, List<String>> a, Map.Entry<String, List<String>> b) {
                        return a.getKey().compareToIgnoreCase(b.getKey());
                    }

                    private boolean isEmpty(Map.Entry<String, List<String>> input) {
                        return input.getValue() == null || input.getValue().isEmpty() || input.getValue().get(0) == null;
                    }
                });
            } else {
                Collections.sort(entries, new Comparator<Map.Entry<String, List<String>>>() {
                    @Override
                    public int compare(Map.Entry<String, List<String>> a, Map.Entry<String, List<String>> b) {
                        return a.getKey().compareToIgnoreCase(b.getKey());
                    }
                });
            }
            for (Map.Entry<String, List<String>> entry : entries) {
                if (entry.getValue() != null) {
                    for (String value : entry.getValue()) {
                        table.addRow(entry.getKey(), value);
                    }
                }
            }
        }
        return format(table);
    }

    /**
     * Renders a 3 columns wide table with the given headers and rows. If headers are provided it should match with the
     * number of columns.
     *
     * @param rows    rows of the table, value map will be added as the last 2 columns to the table
     * @param headers headers of the table
     * @return formatted table
     */
    public static String renderMapValueMap(Map<String, Map<String, String>> rows, String... headers) {
        Table table = createTable(headers);
        if (rows != null) {
            for (String key1 : rows.keySet()) {
                Map<String, String> values = rows.get(key1);
                if (values != null) {
                    for (String key2 : values.keySet()) {
                        table.addRow(key1, key2, values.get(key2));
                    }
                }
            }
        }
        return format(table);
    }

    public static <E extends Object, F extends Object> String renderObjectMapValueMap(Map<String, Map<E, F>> rows, String... headers) {
        Table table = createTable(headers);
        if (rows != null) {
            for (String key1 : rows.keySet()) {
                Map<E, F> values = rows.get(key1);
                if (values != null) {
                    for (Object key2 : values.keySet()) {
                        table.addRow(key1, key2.toString(), values.get(key2).toString());
                    }
                }
            }
        }
        return format(table);
    }

    public static String renderObjectValueMap(Map<String, Object> rows, String mainHeader) {
        Table table = new Table();
        List<String> mainHeaders = new ArrayList<>();
        if (rows != null) {
            int index = 0;
            for (String key1 : rows.keySet()) {
                Object value = rows.get(key1);
                if (value != null) {
                    Field[] fields = value.getClass().getDeclaredFields();
                    if (index == 0) {
                        List<String> headers = new ArrayList<>();
                        headers.add(mainHeader);
                        for (Field classField : fields) {
                            if (!classField.getName().contains("jacoco")) {
                                headers.add(classField.getName());
                                mainHeaders.add(classField.getName());
                            }
                        }
                        table = createTable(headers.toArray(new String[headers.size()]));
                    }
                    List<String> rowValues = new ArrayList<>();
                    rowValues.add(key1);
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

    public static Object runGetter(Field field, Object o) {
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

    private static Map<String, List<String>> convert(Map<String, String> map) {
        Map<String, List<String>> result = new HashMap<String, List<String>>(map.size());
        if (map != null) {
            for (String key : map.keySet()) {
                if (map.get(key) != null) {
                    result.put(key, singletonList(map.get(key)));
                }
            }
        }
        return result;
    }

    private static <E extends Object> Map<String, List<String>> convertObjectMap(Map<String, E> map) {
        Map<String, List<String>> result = new HashMap<String, List<String>>(map.size());
        if (map != null) {
            for (String key : map.keySet()) {
                if (map.get(key) != null) {
                    result.put(key, singletonList(map.get(key).toString()));
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
