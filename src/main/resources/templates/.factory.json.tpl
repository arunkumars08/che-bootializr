{
  "v": "4.0",
  "workspace": {
    "environments": {
      "default": {
        "recipe": {
          "location": "{{dockerImage}}",
          "type": "dockerimage"
        },
        "machines": {
          "dev-machine": {
            "servers": {},
            "agents": [
              "org.eclipse.che.terminal",
              "org.eclipse.che.ws-agent",
              "org.eclipse.che.exec"
            ],
            "attributes": {
              "memoryLimitBytes": "2147483648"
            }
          }
        }
      }
    },
    "defaultEnv": "default",
    "projects": [
      {
        "links": [],
        "description": "{{description}}",
        "source": {
          "location": "{{githubRepoUrl}}",
          "type": "git",
          "parameters": {}
        },
        "mixins": [
          "git",
          "pullrequest"
        ],
        "problems": [],
        "name": "{{artifactId}}",
        "type": "maven",
        "path": "/{{artifactId}}",
        "attributes": {
          "language": [
            "java"
          ]
        }
      }
    ],
    "name": "{{artifactId}}",
    "commands": [
      {
        "commandLine": "scl enable rh-maven33 'mvn install -f ${current.project.path}'",
        "name": "build",
        "type": "mvn",
        "attributes": {
          "goal": "Build",
          "previewUrl": ""
        }
      },
      {
        "commandLine": "scl enable rh-maven33 'mvn clean install -f ${current.project.path}'",
        "name": "clean build",
        "type": "mvn",
        "attributes": {
          "goal": "Build",
          "previewUrl": ""
        }
      },
      {
        "commandLine": "scl enable rh-maven33 'mvn compile spring-boot:run -f ${current.project.path}'",
        "name": "run",
        "type": "custom",
        "attributes": {
          "goal": "Run",
          "previewUrl": "http://${server.port.8080}"
        }
      },
      {
        "commandLine": "scl enable rh-maven33 'mvn spring-boot:run -Drun.jvmArguments=\"-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005\" -f ${current.project.path}'",
        "name": "debug",
        "type": "custom",
        "attributes": {
          "goal": "Debug",
          "previewUrl": "http://${server.port.8080}"
        }
      }
    ]
  },
  "policies": {
    "create": "perClick"
  },
  "ide": {
    "onProjectsLoaded": {
      "actions": [
        {
          "id": "openFile",
          "properties": {
            "file": "/{{artifactId}}/pom.xml"
          }
        }
      ]
    }
  },
  "creator": {
    "name": "Kamesh Sampath",
    "email": "kamesh.sampath@hotmail.com"
  }
}
