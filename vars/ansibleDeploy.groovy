def call() {

    node {

        def config

        stage('Clone') {
            echo 'Cloning Kubernetes repository...'

            checkout scm

            echo 'Repository cloned successfully.'
        }

        stage('Read Configuration') {
            echo 'Reading config.properties...'

            config = readProperties file: 'config.properties'

            echo "Environment: ${config.ENVIRONMENT}"
            echo "Code Base Path: ${config.CODE_BASE_PATH}"
            echo "Playbook: ${config.PLAYBOOK}"
            echo "Inventory: ${config.INVENTORY}"
        }

        stage('User Approval') {

            if (config.KEEP_APPROVAL_STAGE.toBoolean()) {

                input(
                    message: "Deploy ${config.ENVIRONMENT} using ${config.PLAYBOOK}?",
                    ok: 'Proceed'
                )

            } else {
                echo 'Approval stage disabled.'
            }
        }

        stage('Playbook Execution') {

            echo 'Executing Ansible playbook...'

            sh """
                chmod 400 LVM.pem

                ansible-playbook \
                -i ${config.INVENTORY} \
                ${config.PLAYBOOK}
            """

            echo 'Ansible playbook executed successfully.'
        }

        stage('Notification') {

            echo "${config.ACTION_MESSAGE}"

            echo "Job: ${env.JOB_NAME}"
            echo "Build: ${env.BUILD_NUMBER}"
            echo "Environment: ${config.ENVIRONMENT}"
            echo "Slack Channel: ${config.SLACK_CHANNEL_NAME}"
            echo "Build URL: ${env.BUILD_URL}"
        }
    }
}
