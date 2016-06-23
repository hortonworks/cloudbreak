package com.sequenceiq.cloudbreak

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

import org.springframework.core.io.ClassPathResource

class VersionedApplication private constructor() {

    fun showVersionInfo(args: Array<String>): Boolean {
        if (checkIfParamVersion(args)) {
            try {
                if (LONG_VERSION == args[0]) {
                    println("The application info is: \n" + readVersionFromClasspath("application.properties", false))
                } else if (SHORT_VERSION == args[0]) {
                    println(readVersionFromClasspath("application.properties", true))
                }
            } catch (ex: IOException) {
                println("The application.properties file not found version is undefined.")
            }

            return true
        }
        return false
    }

    private fun checkIfParamVersion(args: Array<String>): Boolean {
        return args.size == 1 && (LONG_VERSION == args[0] || SHORT_VERSION == args[0])
    }

    @Throws(IOException::class)
    private fun readVersionFromClasspath(fileName: String, onlyVersion: Boolean): String {
        val sb = StringBuilder()
        val br: BufferedReader
        br = BufferedReader(InputStreamReader(ClassPathResource(fileName).inputStream, "UTF-8"))
        var line: String? = null
        while ((line = br.readLine()) != null) {
            if (onlyVersion) {
                if (line!!.startsWith("info.app.version=")) {
                    line = line.replace("info.app.version=".toRegex(), "")
                    return line
                }
            } else {
                sb.append(line!! + "\n")
            }
        }
        return sb.toString()
    }

    companion object {

        val LONG_VERSION = "--version"
        val SHORT_VERSION = "-v"

        fun versionedApplication(): VersionedApplication {
            return VersionedApplication()
        }
    }
}
