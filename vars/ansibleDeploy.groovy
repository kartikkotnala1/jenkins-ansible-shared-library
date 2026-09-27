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
                        message: "Approve ${environment} deployment?",
                        ok: "Approve"
                    )
                }
            }

            stage('Playbook Execution') {
                steps {
                    echo "Environment: ${environment}"
                    echo "Code Base Path: ${codeBasePath}"

                    sh '''
                        ansible-playbook -i inventory playbook.yml
                    '''
                }
            }

            stage('Notification') {
                steps {
                    echo "Notification"
                    echo "Channel: ${slackChannel}"
                    echo "Message: ${actionMessage}"
                }
            }
        }

        post {
            success {
                echo "Ansible deployment completed successfully."
            }

            failure {
                echo "Ansible deployment failed."
            }
        }
    }
}
