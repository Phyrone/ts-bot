pipeline {
  agent any
  stages {
    stage('Clean') {
      steps {
        sh 'gradle clean'
      }
    }
    stage('JAR') {
      steps {
        sh 'gradle shadowJar'
      }
    }
    stage('Archive') {
      steps {
        archiveArtifacts(artifacts: '**/build/libs/*.jar')
      }
    }
  }
  tools {
    jdk 'openjdk11'
    gradle 'gradle5'
  }
}
