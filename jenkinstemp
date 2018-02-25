pipeline {
  agent any
  environment{

    DEPLOY_TARGET_DIR = "/opt/poc"
    TEMP = "/opt/toProd"
    DEPLOY_PLAY_SCRIPT_DIR = "/opt/ansible/deploy/"
    DEPLOY_PLAY_SCRIPT = "run-deploy-playbook.sh"
    DEPLOY_TARGET_HOST = "localhost"

  }
  stages {
    stage('Config System') {
      steps {
        echo "${env.BRANCH_NAME}"
        echo 'Put here provisioning stuff'
        echo 'Ansible ??'
      }
    }
    stage('Test the System') {
      steps {
        echo 'SBT manage dependencies, just test if is reachable'
        sh 'java -version'
        sh 'sbt about'
      }
    }
    stage('Unit Tests') {
      steps {
        echo 'Tests'
        sh 'sbt clean test'
        archiveArtifacts 'target/test-reports/*.xml'
        junit(testResults: 'target/test-reports/DevOpsPOCSpec.xml',
                allowEmptyResults: true)
      }
    }
    stage('Build') {
      steps {
        echo 'Build'
        sh 'sbt clean compile package assembly'
        archiveArtifacts 'target/scala-*/*.jar'
      }
    }
    stage('Notify') {
      steps {
        script {
          if (env.BRANCH_NAME != "master") {
            sh "git checkout ${env.BRANCH_NAME}"
            sh 'source /etc/profile.d/exports.sh &&' +
                    ' /opt/hub-linux-386-2.3.0-pre10/bin/hub pull-request' +
                    ' -m "$(git log -1 --pretty=%B)"'
            notifyMessage = "Pull Request Sent"
          }
          else {
            echo "Master Branch, nothing to merge"
          }
        }
      }
    }
    stage('Deploy ?') {
      steps{
        script{
          if(env.BRANCH_NAME == "master") {
            header = "Job <${env.JOB_URL}|${env.JOB_NAME}>" +
                    " <${env.JOB_URL}|${env.BRANCH_NAME}>" +
                    " <${env.JOB_DISPLAY_URL}|(Blue)>"
            header += " build <${env.BUILD_URL}|${env.BUILD_DISPLAY_NAME}>" +
                    " <${env.RUN_DISPLAY_URL}|(Blue)>:"
            message = "${header}\n"
            author = sh(script: "git log -1 --pretty=%an", returnStdout: true).trim()
            commitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
            message += " Commit by <@${author}> (${author}): ``` ${commitMessage} ``` "
            message += "--------------------------------------------------------------"
            message += "\nAll Test Done, Build ready for production"
            message += "\n**Deploy ???**"
            message += "\nThis session will be available for 60 second, make a CHOICE!"
            message += "\nPlease <${env.RUN_DISPLAY_URL}|Manual Deploy> it if you want!"
            color = '#36ABCC'
            slackSend(message: message,
                    baseUrl: 'https://devops-pasquali-cm.slack.com' +
                            '/services/hooks/jenkins-ci/',
                    color: color, token: 'ihoCVUPB7hqGz2xI1htD8x0F')

            try {
              timeout(time: 60, unit: 'SECONDS') { // change to a convenient timeout for you
                userInput = input(
                        id: 'DeployPOC', message: 'Deploy in Production??')
              }
            } catch (err) { // timeout reached or input false
              failMessage = "Deploy session expired or aborted"
              error("Deploy session expired or aborted")
            }
          }
          else{
            echo "Nothng To Do"
          }
        }
      }
    }
    stage('Production Deploy') {
      steps {
        script{
          if(env.BRANCH_NAME == "master") {
            echo 'Safe to Deploy in Production, Great Job :D'
            sh "sudo cp target/*/*.jar ${TEMP}"
            sh "sudo cp -Rf conf/* ${TEMP}"
            sh "sudo ${DEPLOY_PLAY_SCRIPT_DIR}${DEPLOY_PLAY_SCRIPT} ${DEPLOY_TARGET_HOST} ${TEMP} ${DEPLOY_TARGET_DIR}"
          }
          else{
            echo "Nothng To Do"
          }
        }
      }
    }
  }
  post {
    success {
      script {
        header = "Job <${env.JOB_URL}|${env.JOB_NAME}> <${env.JOB_DISPLAY_URL}|(Blue)>"
        header += " build <${env.BUILD_URL}|${env.BUILD_DISPLAY_NAME}> <${env.RUN_DISPLAY_URL}|(Blue)>:"
        message = "${header}\n :smiley: All test passed :smiley: $notifyMessage"
        
        author = sh(script: "git log -1 --pretty=%an", returnStdout: true).trim()
        commitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
        message += " Commit by <@${author}> (${author}): ``` ${commitMessage} ``` "
        color = '#00CC00'
        slackSend(message: message,
                baseUrl: 'https://devops-pasquali-cm.slack.com/services/hooks/jenkins-ci/',
                color: color, token: 'ihoCVUPB7hqGz2xI1htD8x0F')
      }
    }
    
    failure {
      script {
        header = "Job <${env.JOB_URL}|${env.JOB_NAME}> <${env.JOB_DISPLAY_URL}|(Blue)>"
        header += " build <${env.BUILD_URL}|${env.BUILD_DISPLAY_NAME}> <${env.RUN_DISPLAY_URL}|(Blue)>:"
        message = "${header}\n :sob: The Build Failed, Release not ready for production! :sob: : ``` ${failMessage} ```\n"
        
        author = sh(script: "git log -1 --pretty=%an", returnStdout: true).trim()
        commitMessage = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
        message += " Commit by <@${author}> (${author}): ``` ${commitMessage} ``` "
        color = '#990000'
        slackSend(message: message,
                baseUrl: 'https://devops-pasquali-cm.slack.com/services/hooks/jenkins-ci/',
                color: color, token: 'ihoCVUPB7hqGz2xI1htD8x0F')
      }
    }
    
  }
}