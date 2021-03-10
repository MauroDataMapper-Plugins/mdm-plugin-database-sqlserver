pipeline {
    agent any

    environment {
        JENKINS = 'true'
    }

    tools {
        jdk 'jdk-12'
    }

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        skipStagesAfterUnstable()
        buildDiscarder(logRotator(numToKeepStr: '30'))
    }

    stages {

        stage('Clean') {
            // Only clean when the last build failed
            when {
                expression {
                    currentBuild.previousBuild?.currentResult == 'FAILURE'
                }
            }
            steps {
                sh "./gradlew clean"
            }
        }

        stage('Info') {
            steps {
                sh './gradlew -v' // Output gradle version for verification checks
                sh './gradlew jvmArgs sysProps'
                sh './grailsw -v' // Output grails version for verification checks
            }
        }

        stage('Test cleanup & Compile') {
            steps {
                sh "./gradlew jenkinsClean"
                sh './gradlew compile'
            }
        }

        stage('License Header Check') {
            steps {
                warnError('Missing License Headers') {
                    sh './gradlew --build-cache license'
                }
            }
        }

        stage('Unit Test') {

            steps {
                sh "./gradlew --build-cache test"
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
                }
            }
        }

        stage('Integration Test') {

            steps {
                sh "./gradlew --build-cache integrationTest"
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'build/test-results/integrationTest/*.xml'
                }
            }
        }

        stage('Static Code Analysis') {
            steps {
                sh "./gradlew -PciRun=true staticCodeAnalysis jacocoTestReport"
            }
        }

        stage('Sonarqube') {
            when {
                branch 'develop'
            }
            steps {
                withSonarQubeEnv('JenkinsQube') {
                    sh "./gradlew sonarqube"
                }
            }
        }

        stage('Deploy to Artifactory') {
            when {
                allOf {
                    anyOf {
                        branch 'main'
                        branch 'develop'
                    }
                    expression {
                        currentBuild.currentResult == 'SUCCESS'
                    }
                }

            }
            steps {
                script {
                    sh "./gradlew artifactoryPublish"
                }
            }
        }
    }

    post {
        always {
            publishHTML([
                allowMissing         : false,
                alwaysLinkToLastBuild: true,
                keepAll              : true,
                reportDir            : 'build/reports/tests',
                reportFiles          : 'index.html',
                reportName           : 'Test Report',
                reportTitles         : 'Test'
            ])

            recordIssues enabledForFailure: true, tools: [java(), javaDoc()]
            recordIssues enabledForFailure: true, tool: checkStyle(pattern: '**/reports/checkstyle/*.xml')
            recordIssues enabledForFailure: true, tool: codeNarc(pattern: '**/reports/codenarc/*.xml')
            recordIssues enabledForFailure: true, tool: spotBugs(pattern: '**/reports/spotbugs/*.xml', useRankAsPriority: true)
            recordIssues enabledForFailure: true, tool: pmdParser(pattern: '**/reports/pmd/*.xml')

            publishCoverage adapters: [jacocoAdapter('**/reports/jacoco/jacocoTestReport.xml')]
            outputTestResults()
            jacoco classPattern: '**/build/classes', execPattern: '**/build/jacoco/*.exec', sourceInclusionPattern: '**/*.java,**/*.groovy', sourcePattern: '**/src/main/groovy,**/grails-app/controllers,**/grails-app/domain,**/grails-app/services,**/grails-app/utils'
            archiveArtifacts allowEmptyArchive: true, artifacts: '**/*.log'
            slackNotification()
        }
    }
}