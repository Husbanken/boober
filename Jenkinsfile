def jenkinsfile
def version='feature/AOS-2372'
fileLoader.withGit('https://git.aurora.skead.no/scm/ao/aurora-pipeline-scripts.git', version) {
   jenkinsfile = fileLoader.load('templates/leveransepakke')
}

def overrides = [
    piTests: false,
    checkstyle: false,
    docs: false,
    sonarqube: false,
    credentialsId: "github"
]

jenkinsfile.gradle(version, overrides)
