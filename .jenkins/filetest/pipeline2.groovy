pipeline {
    agent any

    parameters {
        string(name: 'VERSION', defaultValue: '', description: 'Version of Cloudbreak.')
    }

    stages {
        stage('Using parameter to do something') {
            steps {
                sh "The received version is: "
                sh "echo '${params.VERSION}'"
            }
        }
    }
}
