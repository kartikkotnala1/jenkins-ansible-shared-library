def call(String configFile = 'config.properties') {

    def config = readProperties file: configFile

    def slackChannel = config.SLACK_CHANNEL_NAME
    def environment = config.ENVIRONMENT
    def codeBasePath = config.CODE_BASE_PATH
    def actionMessage = config.ACTION_MESSAGE
    def keepApprovalStage = config.KEEP_APPROVAL_STAGE.toBoolean()

    pipeline {

        agent any

        stages {

            stage('Clone') {
                steps {
                    echo "Cloning Kubernetes repository..."

                    git(
                        branch: 'main',
                        url: 'https://github.com/kartikkotnala1/Kubernetes.git'
                    )

                    echo "Repository cloned successfully."
                }
            }

            stage('Read Configuration') {
                steps {
                    echo "Reading config.properties..."

                    echo "Environment: ${environment}"
                    echo "Code Base Path: ${codeBasePath}"
                    echo "Slack Channel: ${slackChannel}"
                }
            }

            stage('User Approval') {
                when {
                    expression {
                        return keepApprovalStage
                    }
                }

                steps {
                    input(
                        message: "Deploy ${environment} using Ansible?",
                        ok: "Proceed"
                    )
                }
            }

            stage('Playbook Execution') {
                steps {
                    echo "Executing Ansible playbook..."

                    sh '''
                        chmod 400 LVM.pem
                        ansible-playbook -i inventory playbook.yml
                    '''

                    echo "Ansible playbook executed successfully."
                }
            }

            stage('Notification') {
                steps {
                    script {

                        def message = """
Kubernetes Ansible Deployment

Status: SUCCESS
Environment: ${environment}
Message: ${actionMessage}
Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Build URL: ${env.BUILD_URL}
"""

                        echo "Sending Slack notification..."
                        echo "Slack Channel: ${slackChannel}"

                        slackSend(
                            channel: slackChannel,
                            message: message
                        )

                        echo "Slack notification sent successfully."
                    }
                }
            }
        }

        post {

            success {
                echo "Kubernetes deployment completed successfully."
            }

            failure {
                script {

                    def failureMessage = """
Kubernetes Ansible Deployment

Status: FAILED
Environment: ${environment}
Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Build URL: ${env.BUILD_URL}
"""

                    slackSend(
                        channel: slackChannel,
                        message: failureMessage
                    )
                }
            }
        }
    }
}
