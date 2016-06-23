package com.sequenceiq.it.ssh

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.FileSystem
import java.nio.file.Path

import org.apache.commons.io.IOUtils
import org.apache.sshd.common.file.FileSystemFactory
import org.apache.sshd.common.file.util.MockFileSystem
import org.apache.sshd.common.session.Session
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource

class MockFileSystemFactory : FileSystemFactory {

    @Throws(IOException::class)
    override fun createFileSystem(session: Session): FileSystem {
        return createMockFileSystem()
    }

    private fun createMockFileSystem(): FileSystem {
        return object : MockFileSystem("mockfs") {
            override fun getPath(first: String?, vararg more: String): Path {
                val fileName = File(first).toPath().fileName.toString()
                try {
                    val classPathResource = ClassPathResource(fileName)
                    val inputStream = classPathResource.inputStream
                    val tempFile = File(fileName)
                    try {
                        FileOutputStream(tempFile).use { outputStream -> IOUtils.copy(inputStream, outputStream) }
                    } catch (e: IOException) {
                        LOGGER.error("can't write " + fileName, e)
                    }

                    return tempFile.toPath()
                } catch (e: IOException) {
                    LOGGER.info("can't retrieve path from classpath, let's return with a file path from working directory")
                    return File(first).toPath().fileName
                }

            }
        }
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(MockFileSystemFactory::class.java)
    }
}
