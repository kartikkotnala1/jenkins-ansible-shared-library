```groovy
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

            echo "Playbook: ${config.playbook}"
            echo "Inventory: ${config.inventory}"
        }

        stage('User Approval') {
            input(
                message: 'Do you want to execute the Ansible playbook?',
                ok: 'Proceed'
            )
        }

        stage('Playbook Execution') {
            echo 'Executing Ansible playbook...'

            sh """
                ansible-playbook \
                -i ${config.inventory} \
                ${config.playbook}
            """

            echo 'Ansible playbook executed successfully.'
        }

        stage('Notification') {
            echo 'Ansible deployment completed successfully.'

            echo "Job: ${env.JOB_NAME}"
            echo "Build: ${env.BUILD_NUMBER}"
            echo "Build URL: ${env.BUILD_URL}"
        }
    }
}
```
