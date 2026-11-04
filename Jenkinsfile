pipeline {
  agent any
  tools { maven 'maven' }
  triggers { githubPush() }
  environment {
    IMAGE = "senthilkumarsoundararajan/demo-app"
    TAG   = "v${BUILD_NUMBER}"
  }
  stages {

    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Maven Build') {
      steps { sh 'mvn -B clean package -DskipTests' }
    }

    stage('SonarQube Analysis') {
      steps {
        withSonarQubeEnv('sonar') {
            sh 'mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:3.9.1.2184:sonar -Dsonar.projectKey=demo-app'
        }
      }
    }

    stage('Quality Gate') {
      steps {
        timeout(time: 5, unit: 'MINUTES') {
          waitForQualityGate abortPipeline: true
        }
      }
    }

    stage('Tests') {
      steps { sh 'mvn -B test' }
    }

    stage('Docker Build') {
      steps { sh 'docker build -t $IMAGE:$TAG .' }
    }

    stage('Push to Docker Hub') {
      steps {
        withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DH_USER', passwordVariable: 'DH_PASS')]) {
          sh 'echo "$DH_PASS" | docker login -u "$DH_USER" --password-stdin'
          sh 'docker push $IMAGE:$TAG'
        }
      }
    }

    stage('Image Updater - Manifests Repo') {
      steps {
        sh '''
          for i in $(seq 1 30); do
            rm -rf m && git clone -q https://github.com/creativesenthil/demo-manifests.git m
            if grep -rq "$TAG" m --exclude-dir=.git; then
              echo "Manifests repo updated with $TAG"; exit 0
            fi
            echo "Waiting for Image Updater to update the manifests repo..."; sleep 20
          done
          echo "Manifests repo was not updated"; exit 1
        '''
      }
    }

    stage('Argo CD Sync') {
      steps {
        sh '''
          for i in $(seq 1 30); do
            IMGS=$(kubectl -n argocd get app demo-app -o jsonpath='{.status.summary.images}')
            SYNC=$(kubectl -n argocd get app demo-app -o jsonpath='{.status.sync.status}')
            HEALTH=$(kubectl -n argocd get app demo-app -o jsonpath='{.status.health.status}')
            echo "Sync=$SYNC Health=$HEALTH"
            if echo "$IMGS" | grep -q "$TAG" && [ "$SYNC" = "Synced" ] && [ "$HEALTH" = "Healthy" ]; then
              exit 0
            fi
            sleep 15
          done
          echo "Argo CD did not sync in time"; exit 1
        '''
      }
    }

    stage('Kubernetes Deploy') {
      steps {
        sh '''
          kubectl -n demo rollout status deploy/demo-app --timeout=180s
          kubectl -n demo get pods
        '''
      }
    }
  }

  post {
    success { echo "Pipeline succeeded: $IMAGE:$TAG deployed" }
    failure {
      script {
        try {
          withCredentials([string(credentialsId: 'slack-webhook', variable: 'HOOK')]) {
            sh '''curl -s -X POST -H "Content-type: application/json" --data "{\\"text\\":\\"Pipeline FAILED: $JOB_NAME #$BUILD_NUMBER $BUILD_URL\\"}" "$HOOK"'''
          }
        } catch (err) {
          echo "Pipeline FAILED (add a 'slack-webhook' secret-text credential to get Slack alerts)"
        }
      }
    }
  }
}
