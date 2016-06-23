package com.sequenceiq.periscope.utils

import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource

import com.google.common.io.BaseEncoding

object FileReaderUtils {

    private val LOGGER = LoggerFactory.getLogger(FileReaderUtils::class.java)

    fun readFileFromClasspathQuietly(fileName: String): String? {
        try {
            return readFileFromClasspath(fileName)
        } catch (e: IOException) {
            LOGGER.warn("Failed to load file from classpath", e)
            return null
        }

    }

    @Throws(IOException::class)
    fun readFileFromClasspath(fileName: String): String {
        val sb = StringBuilder()
        val br: BufferedReader
        br = BufferedReader(InputStreamReader(ClassPathResource(fileName).inputStream, "UTF-8"))
        var c = br.read()
        while (c != -1) {
            sb.append(c.toChar())
            c = br.read()
        }
        return sb.toString()
    }

    @Throws(IOException::class)
    fun readFileFromPath(fileName: String): String {
        val br = IOUtils.toString(FileInputStream(fileName))
        return BaseEncoding.base64().encode(br.toByteArray())
    }

    @Throws(IOException::class)
    fun readBinaryFileFromPath(fileName: String): String {
        val path = Paths.get(fileName)
        return BaseEncoding.base64().encode(Files.readAllBytes(path))
    }

    @Throws(IOException::class)
    fun readFileFromPathToString(fileName: String): String {
        val path = Paths.get(fileName)
        return String(Files.readAllBytes(path))
    }

}
