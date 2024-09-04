# Sonatype Central Repository Gradle publishing plugin

[Gradle](https://gradle.org/) plugin for publishing artifacts to [Sonatype Maven Central](https://central.sonatype.com/) using [Portal Publisher API](https://central.sonatype.org/publish/publish-portal-api/)

# Java project setup example

`build.gradle`:

```gradle
plugins {
    id "java"
    id "io.github.boolivar.sonatype-portal-publish" version "0.1.0"
}

group = "com.example.applications"

publishing {
    publications {
        maven(MavenPublication) {
            from components.java
            pom {
                name = "ossrh-demo"
                description = "A demo for deployment to the Central Repository via OSSRH"
                url = "http://github.com/simpligility/ossrh-demo"
                licenses {
                    license {
                        name = "The Apache Software License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        name = "Manfred Moser"
                        email = "manfred@sonatype.com"
                        organization = "Sonatype"
                        organizationUrl = "http://www.sonatype.com"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/simpligility/ossrh-demo.git"
                    developerConnection = "scm:git:ssh://github.com:simpligility/ossrh-demo.git"
                    url = "http://github.com/simpligility/ossrh-demo/tree/master"
                }
            }
        }
    }
}
```

```bash
export ORG_GRADLE_PROJECT_sonatypeMavenCentralUser=<sonatype username>
export ORG_GRADLE_PROJECT_sonatypeMavenCentralPassword=<sonatype password>
export ORG_GRADLE_PROJECT_sonatypeSigningKey=`cat ~/private-key.pem`
export ORG_GRADLE_PROJECT_sonatypeSigningSecret=<signing secret>
```

```bash
./gradlew publishToSonatype
```
