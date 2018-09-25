pipeline {
    agent { label 'gradle-build-and-int-test' }

    stages {
        stage('Build') {
            steps {
                sh '''
                    ./gradlew -Penv=jenkins -b build.gradle clean build --info --stacktrace --parallel -x test -x checkstyleMain -x checkstyleTest -x spotbugsMain -x spotbugsTest
                '''
            }
        }
        stage('Spotbugs Main') {
            steps {
                sh '''
                    ./gradlew -Penv=jenkins -b build.gradle check -x checkstyleMain -x checkstyleTest -x spotbugsTest -x test --no-daemon --stacktrace
                '''
            }
        }
        stage('Checkstyle Main') {
            steps {
                sh '''
                    ./gradlew -Penv=jenkins -b build.gradle check -x spotbugsMain -x spotbugsTest -x checkstyleTest -x test --no-daemon --stacktrace
                '''
            }
        }
        stage('Spotbugs Test') {
            steps {
                sh '''
                    ./gradlew -Penv=jenkins -b build.gradle check -x checkstyleMain -x checkstyleTest -x spotbugsMain -x test --no-daemon --stacktrace
                '''
            }
        }
        stage('Checkstyle Test') {
            steps {
                sh '''
                    ./gradlew -Penv=jenkins -b build.gradle check -x spotbugsMain -x spotbugsTest -x checkstyleMain -x test --no-daemon --stacktrace
                '''
            }
        }
        stage('Unit Test') {
            steps {
                sh '''
                    ./gradlew -Penv=jenkins -b build.gradle test jacocoTestReport --info --stacktrace --parallel -x checkstyleMain -x checkstyleTest -x spotbugsMain -x spotbugsTest --no-daemon
                '''
            }
        }
    }
}