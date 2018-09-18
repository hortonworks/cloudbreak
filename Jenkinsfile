pipeline {
    agent any

    stages {
        stage ('Init') {
            steps {
                sh 'export AWS_ACCESS_KEY_ID=$S3_AWS_ACCESS_KEY_ID'
                sh 'export AWS_ACCESS_KEY=$S3_AWS_ACCESS_KEY_ID'
                sh 'export AWS_SECRET_ACCESS_KEY=$S3_AWS_SECRET_ACCESS_KEY'
                sh 'export JAVA_HOME=/usr/lib/jvm/jdk-10.0.1'
            }
        }
        stage ('Build') {
             steps {
                sh "echo 'java version is: '; java -version"
                sh './gradlew -Penv=jenkins -b build.gradle clean build --info --stacktrace --parallel'
            }
        }
    }
}