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
    jdk 'Java8'
    gradle 'gradle5'
  }
}
