def call(String configFile = 'config.properties') {

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
                    script {

                        def config = readProperties file: configFile

                        env.SLACK_CHANNEL = config.SLACK_CHANNEL_NAME
                        env.ENVIRONMENT = config.ENVIRONMENT
                        env.CODE_BASE_PATH = config.CODE_BASE_PATH
                        env.ACTION_MESSAGE = config.ACTION_MESSAGE
                        env.KEEP_APPROVAL_STAGE = config.KEEP_APPROVAL_STAGE
                        env.PLAYBOOK = config.PLAYBOOK
                        env.INVENTORY = config.INVENTORY

                        echo "Reading ${configFile}..."

                        echo "Environment: ${env.ENVIRONMENT}"
                        echo "Code Base Path: ${env.CODE_BASE_PATH}"
                        echo "Slack Channel: ${env.SLACK_CHANNEL}"
                        echo "Approval Stage: ${env.KEEP_APPROVAL_STAGE}"
                        echo "Playbook: ${env.PLAYBOOK}"
                        echo "Inventory: ${env.INVENTORY}"
                    }
                }
            }

            stage('User Approval') {
                when {
                    expression {
                        return env.KEEP_APPROVAL_STAGE?.toBoolean()
                    }
                }

                steps {
                    input(
                        message: "Deploy ${env.ENVIRONMENT} using ${env.CODE_BASE_PATH}?",
                        ok: "Proceed"
                    )
                }
            }

            stage('Playbook Execution') {
                steps {
                    echo "Executing Ansible playbook..."

                    sh """
                        chmod 400 LVM.pem
                        ansible-playbook -i ${env.INVENTORY} ${env.PLAYBOOK}
                    """

                    echo "Ansible playbook executed successfully."
                }
            }

            stage('Notification') {
                steps {
                    script {

                        if (!env.SLACK_CHANNEL?.trim()) {
                            error "SLACK_CHANNEL_NAME is missing in config.properties"
                        }

                        def successMessage = """
Kubernetes Ansible Deployment

Status: SUCCESS
Environment: ${env.ENVIRONMENT}
Message: ${env.ACTION_MESSAGE}
Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Build URL: ${env.BUILD_URL}
"""

                        echo "Sending Slack notification..."
                        echo "Slack Channel: ${env.SLACK_CHANNEL}"

                        slackSend(
                            channel: env.SLACK_CHANNEL,
                            message: successMessage
                        )

                        echo "Slack notification sent successfully."
                    }
                }
            }
        }

        post {
            failure {
                script {

                    if (env.SLACK_CHANNEL?.trim()) {

                        def failureMessage = """
Kubernetes Ansible Deployment

Status: FAILED
Environment: ${env.ENVIRONMENT}
Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
Build URL: ${env.BUILD_URL}
"""

                        slackSend(
                            channel: env.SLACK_CHANNEL,
                            message: failureMessage
                        )
                    }
                }
            }
        }
    }
}
