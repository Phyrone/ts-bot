pipeline {
  agent any
  stages {
    stage('Clean') {
      steps {
        sh './gradlew clean'
      }
    }
    stage('JAR') {
      steps {
        sh './gradlew shadowJar'
      }
    }
    stage('Archive') {
      steps {
        archiveArtifacts(artifacts: '**/build/libs/*.jar')
      }
    }
  }
  tools {
    jdk 'Java8'
  }
}