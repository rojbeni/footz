@Library('sym-pipeline') _


import com.symphony.cicd.SymphonyCICDUtils
import com.symphony.cicd.deploy.K8sUtils

def util = new SymphonyCICDUtils()
def k8sUtils = new K8sUtils()
def isPullRequest = util.isPullRequest()

env.USE_OPENJDK8 = env.USE_OPENJDK8 ?: false
nodeName = "jnlp-openjdk8"
if (env.USE_OPENJDK8.toBoolean() == false) {
    nodeName = null
}

node(nodeName) {
    def projectRepo = env.PROJECT_REPO ?: "symphony-agent"
    def projectOrg = env.PROJECT_ORG ?: "SymphonyOSF"
    def projectBranch = env.BRANCH_NAME ?: "master"
    withEnv(["PROJECT_TYPE=java",
             "GIT_REPO=${projectRepo}",
             "GIT_ORG=${projectOrg}",
             "GIT_BRANCH=${projectBranch}",
             // Disable Maven transfer logs to avoid noise in Jenkins logs
             "MAVEN_OPTS=-Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn",
             "USE_OPENJDK8=true"]) {
        withCredentials([
                [$class: 'StringBinding', credentialsId: 'SNYK_DEVX_TOKEN', variable: 'SNYK_DEVX_TOKEN']
        ]) {
            gitCheckout()
            // Retrieve Maven settings (for Artifactory) from k8s secret which seems to be the most reliable way
            k8sUtils.getSettingsXML('/root/.m2')
            buildProject(util)
            runDockerBuild(k8sUtils, isPullRequest)
            runSecurityMonitoring(isPullRequest)
            runCoverageAnalysis(isPullRequest)
            runQualityAnalysis(isPullRequest)
        }
    }
}

void buildProject(util) {
    stage('Build') {
        try {
            sh "mvn verify -B -P CodeCoverage,JacocoToCobertura -DpathToCover2Cover=/opt/bin/cover2cover.py"
        } finally {
            util.archiveJunitTestArtifacts("**/target/surefire-reports/TEST-*.xml")
        }
    }
}

void runDockerBuild(k8sUtils, isPullRequest) {
    if (!isPullRequest) {
        // This will only run for CI jobs, i.e., jobs for branches that are not
        // part of a PR. For every master push, for example, it's going to create
        // and tag a new image with the branch name.
        stage('Docker Images') {
            // AWS DEV-MT login for base image
            withCredentials([[$class       : 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                              credentialsId: "sym-aws-devmt", secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                sh """
                    aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 115671292914.dkr.ecr.us-east-1.amazonaws.com
                    """
            }
            // GCR login to push image
            sh 'gcloud auth configure-docker'
            k8sUtils.activateGceCreds()

            // Use the branch as a tag
            def branchName = env.CHANGE_BRANCH ?: env.BRANCH_NAME
            // Build and push to Docker registry used by epods
            sh "cd docker && ./build-image.sh ${branchName}"

            // Build and push to production registries
            sh  """
                chmod 755 "./build.sh"
                ./build.sh
            """
            pom = readMavenPom()
            def tag = "${pom.version}-${currentBuild.number}"
            pushAws(tag)
            pushGcp(tag)
        }
    }
}

// Builds a docker image and push it to symphony-gce-dev project registry
void runSecurityMonitoring(isPullRequest) {
    stage('Security monitoring') {
        if (!isPullRequest
                && (env.GIT_BRANCH == "master" || env.GIT_BRANCH ==~ /[0-9]+[.][0-9]+/)) {
            // first publish snapshots locally, then run snyk, initialize is used to retrieve the branch name
            sh "mvn install -DskipTests -P-quality -B && mvn initialize snyk:monitor -N -B"
            // Snyk has been installed by the Maven plugin execution, reuse it for Docker image monitoring
            sh "/root/.local/share/snyk/snyk-linux auth ${env.SNYK_DEVX_TOKEN}"
            sh "/root/.local/share/snyk/snyk-linux container monitor --file=docker/Dockerfile us.gcr.io/symphony-gce-dev/sym/symphony-agent:${GIT_BRANCH}"
        } else {
            println("Monitoring is done only for release branches, skipping it")
        }
    }
}

void runCoverageAnalysis(def isPullRequest) {
    if (isPullRequest) {
        echo "currentBuild.result Before Coverage Analysis - ${currentBuild.result}"
        stage('Coverage Analysis') {
            diffCover(env.CHANGE_TARGET)
        }
        echo "currentBuild.result After Coverage Analysis - ${currentBuild.result}"
    }
}

void runQualityAnalysis(def isPullRequest) {
    echo "currentBuild.result Before Quality Analysis - ${currentBuild.result}"
    stage('Quality Analysis') {
        if (!isPullRequest) {
            // Avoid tagging artifacts when running full sonar scan,
            // because by the time it finishes there may be more recently tagged versions of the artifacts
            writeFile file: "ignore-artifacts-deployed", text: "ignore"
            stash name: "ignore-artifacts-deployed", includes: "**/ignore-artifacts-deployed"
        }
        sonar()
    }
    echo "currentBuild.result After Quality Analysis - ${currentBuild.result}"
}

def pushAws(version) {
    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID',
                      credentialsId: 'sym-aws-dev', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
        sh """
            aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 189141687483.dkr.ecr.us-east-1.amazonaws.com
        """
    }
    def targetImageUrl = "189141687483.dkr.ecr.us-east-1.amazonaws.com/symphony-es/agent:${version}"
    sh """
        docker tag agent ${targetImageUrl}
        docker push ${targetImageUrl}
    """
    echo "Image pushed: ${version} on ${targetImageUrl}"
}

def pushGcp(version) {
    withCredentials([file(credentialsId: 'warpdrive-ci', variable: 'GCR_TOKEN')]) {
        sh """
            cat "\$GCR_TOKEN" | docker login -u _json_key --password-stdin \
                https://us-east4-docker.pkg.dev/sym-prod-mr-tools-01/symphony-agent-docker-us-east4
        """
    }
    def targetImageUrl = "us-east4-docker.pkg.dev/sym-prod-mr-tools-01/symphony-agent-docker-us-east4/agent:${version}"
    sh """
        docker tag agent ${targetImageUrl}
        docker push ${targetImageUrl}
    """
    echo "Image pushed: ${version} on ${targetImageUrl}"
}
