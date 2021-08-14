import groovy.transform.Field
import groovy.json.JsonSlurper
import java.io.IOException
import java.util.Arrays
import java.util.ArrayList
import java.util.Collections
import java.util.Iterator
import java.util.List

@Field def project="example-scan-sonar-project"
@Field def urlSonar="http://10.130.2.40:9000"
@Field def qualitygateId="AXtGVAiSS3H2M_1OZfhu"

pipeline {
    agent any

    stages {
        stage('clone repo') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: '*/master']], extensions: [], userRemoteConfigs: [[url: 'https://github.com/SonarSource/sonar-scanning-examples.git']]])
            }
        }

        stage('create scan-sonar') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    dir('sonarqube-scanner'){
                        createProjectSonar(project,qualitygateId)
                    }
                }
            }
        }
        stage(' analize scan-sonar') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    dir('sonarqube-scanner'){
                    sh "sonar-scanner -Dsonar.java.binaries=. -Dsonar.projectKey=${project}"

                    }
                }
            }
        }

        stage(' validate scan-sonar') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    evaluateScanSonar()
                }
            }
        }
    }
}

def createProjectSonar(projects,qualitygateId){
    withCredentials([string(credentialsId: 'token-sonar', variable: 'token')]) {
        for(def project in projects.split(";")){
            sh """
            curl -u ${token}: -X POST -d 'name='+$project+'&project='+$project+'&visibility=private' '${urlSonar}/api/projects/create'
            curl -u ${token} -X POST "${urlSonar}/api/qualitygates/select?gateId=${qualitygateId}&projectKey=${project}"
            """
        }
    }
}

def evaluateScanSonar(){ 
    timeout(time: 40, unit: 'SECONDS') {
          def qg=waitForQualityGate()
        if (qg.status != 'OK') {
            error "Pipeline aborted due to quality gate failure: ${qg.status}"
           
        }else{
            println "scan success"+qg
        }
    }
}

/*String evaluateScanSonar(){
	def qualityGateSonar = "NO CALCULATED"
	def failed = false
	def validaciones=0
	while(qualityGateSonar=="NO CALCULATED" && validaciones<30){
		try{
			validaciones=validaciones+1
			println("Quality Gate= "+qualityGateSonar+" Validacion #"+validaciones)
			timeout(time: 30, unit: 'SECONDS') {
				qualityGateSonar = waitForQualityGate()
				if (qualityGateSonar.status != 'OK') {
					failed=true
				}
			}
		}catch(Exception e){
			qualityGateSonar = "NO CALCULATED"
		}
	}
	if(!failed){
		return "Quality Gate Passed"
	}else{
		return "Did not pass the Quality Gate"
	}
}*/