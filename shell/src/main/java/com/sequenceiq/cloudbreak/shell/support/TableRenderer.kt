package com.sequenceiq.cloudbreak.shell.support

import java.util.Collections.singletonList

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.HashMap

import org.springframework.shell.support.table.Table
import org.springframework.shell.support.table.TableHeader
import org.springframework.stereotype.Component

/**
 * Utility class used to render tables.
 */
@Component
class TableRenderer {

    /**
     * Renders a 2 columns wide table with the given headers and rows. If headers are provided it should match with the
     * number of columns.

     * @param rows                  rows of the table
     * *
     * @param sortByFirstColumn     sortByFirstColumn rows by the first column
     * *
     * @param headers               headers of the table
     * *
     * @return the formatted table
     */
    fun <E : Any> renderSingleMapWithSortedColumn(rows: Map<E, String>, vararg headers: String): String {
        return renderMultiValueMap(convert(rows), true, *headers)
    }

    /**
     * Renders a 2 columns wide table with the given headers and rows. If headers are provided it should match with the
     * number of columns.

     * @param rows                  rows of the table, each value will be added as a new row with the same key
     * *
     * @param sortByFirstColumn     sortByFirstColumn rows by the first column
     * *
     * @param headers               headers of the table
     * *
     * @return formatted table
     */
    fun renderMultiValueMap(rows: Map<String, List<String>>?, sortByFirstColumn: Boolean, vararg headers: String): String {
        val table = createTable(*headers)
        if (rows != null) {
            val entries = ArrayList(rows.entries)
            if (sortByFirstColumn) {
                Collections.sort(entries, object : Comparator<Entry<String, List<String>>> {
                    override fun compare(a: Entry<String, List<String>>, b: Entry<String, List<String>>): Int {
                        if (isEmpty(a) || isEmpty(b)) {
                            return compareKeys(a, b)
                        }
                        val comp = a.value.get(0).compareTo(b.value.get(0), ignoreCase = true)
                        return if (comp == 0) compareKeys(a, b) else comp
                    }

                    private fun compareKeys(a: Entry<String, List<String>>, b: Entry<String, List<String>>): Int {
                        return a.key.compareTo(b.key, ignoreCase = true)
                    }

                    private fun isEmpty(input: Entry<String, List<String>>): Boolean {
                        return input.value == null || input.value.isEmpty() || input.value.get(0) == null
                    }
                })
            } else {
                Collections.sort(entries) { a, b -> a.key.compareTo(b.key, ignoreCase = true) }
            }
            for (entry in entries) {
                if (entry.value != null) {
                    for (value in entry.value) {
                        table.addRow(entry.key, value)
                    }
                }
            }
        }
        return format(table)
    }

    fun <E : Any> renderObjectValueMap(rows: Map<String, E>?, mainHeader: String): String {
        var table = Table()
        val mainHeaders = ArrayList<String>()
        if (rows != null) {
            var index = 0
            for (key1 in rows.keys) {
                val value = rows[key1]
                if (value != null) {
                    val fields = value.javaClass.getDeclaredFields()
                    if (index == 0) {
                        val headers = ArrayList<String>()
                        headers.add(mainHeader)
                        for (classField in fields) {
                            if (!classField.getName().contains("jacoco")) {
                                headers.add(classField.getName())
                                mainHeaders.add(classField.getName())
                            }
                        }
                        table = createTable(*headers.toArray<String>(arrayOfNulls<String>(headers.size)))
                    }
                    val rowValues = ArrayList<String>()
                    rowValues.add(key1)
                    for (classField in fields) {
                        if (mainHeaders.contains(classField.getName())) {
                            classField.setAccessible(true)
                            val o = runGetter(classField, value)
                            if (o != null) {
                                rowValues.add(o.toString())
                            }
                        }
                    }
                    table.addRow(*rowValues.toArray<String>(arrayOfNulls<String>(rowValues.size)))
                    index++
                }
            }
        }
        return format(table)
    }

    private fun runGetter(field: Field, o: Any): Any? {
        for (method in o.javaClass.getMethods()) {
            if (method.getName().startsWith("get") && method.getName().length == field.name.length + THREE) {
                if (method.getName().toLowerCase().endsWith(field.name.toLowerCase())) {
                    try {
                        return method.invoke(o)
                    } catch (e: Exception) {
                        return null
                    }

                }
            }
        }
        return null
    }

    companion object {

        val THREE = 3


        private fun createTable(vararg headers: String): Table {
            val table = Table()
            if (headers != null) {
                var column = 1
                for (header in headers) {
                    table.addHeader(column++, TableHeader(header))
                }
            }
            return table
        }

        private fun <E : Any> convert(map: Map<E, String>?): Map<String, List<String>> {
            val result = HashMap<String, List<String>>(map!!.size)
            if (map != null) {
                for (key in map.keys) {
                    if (map[key] != null) {
                        result.put(key.toString(), listOf<String>(map[key]))
                    }
                }
            }
            return result
        }

        private fun <E : Any> convertObjectMap(map: Map<String, E>?): Map<String, List<String>> {
            val result = HashMap<String, List<String>>(map!!.size)
            if (map != null) {
                for (key in map.keys) {
                    if (map[key] != null) {
                        result.put(key, listOf<String>(map[key].toString()))
                    }
                }
            }
            return result
        }

        private fun format(table: Table): String {
            table.calculateColumnWidths()
            return table.toString()
        }
    }
}
