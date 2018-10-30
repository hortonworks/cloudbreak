./gradlew -Penv=jenkins -b build.gradle build --info --stacktrace --parallel -x checkstyleMain -x checkstyleTest -x findbugsMain -x findbugsTest
