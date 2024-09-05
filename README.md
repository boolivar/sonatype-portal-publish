# Sonatype Central Repository Gradle publishing plugin

[Gradle](https://gradle.org/) plugin for publishing artifacts to [Sonatype Maven Central](https://central.sonatype.com/) using [Portal Publisher API](https://central.sonatype.org/publish/publish-portal-api/)

## Requirements
- min java version: 8
- min gradle version: 6

## Java project setup example

1. Configure plugin and publication

`build.gradle`:

```gradle
plugins {
    id "java"
    id "io.github.boolivar.sonatype-portal-publish" version "0.1.0"
}

group = "com.simpligility.training"
version = "1.0"

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
2. Setup sonatype [credentials](https://central.sonatype.org/publish/generate-portal-token/) and [signing](https://central.sonatype.org/publish/requirements/gpg/) key using project properties

```bash
export ORG_GRADLE_PROJECT_sonatypeMavenCentralUser=<sonatype username>
export ORG_GRADLE_PROJECT_sonatypeMavenCentralPassword=<sonatype password>
export ORG_GRADLE_PROJECT_sonatypeSigningKey=`cat ~/private-key.pem`
export ORG_GRADLE_PROJECT_sonatypeSigningSecret=<signing secret>
```
3. Run `publishToSonatype` gradle task

```bash
./gradlew publishToSonatype
```

## sonatypePublish extension

`build.gradle` example:

```gradle
sonatypePublish {
    dir = layout.buildDirectory.dir("maven-central-publish")
    bundleName = "awesome-publication"
    autoPublish = true
}
```

| Extension property | Type | Default value | Description |
| ------------------ | ---- | ------------- | ----------- |
| `dir` | `Directory` | `$buildDir/sonatypePublish` | Output directory for storing publication artifacts and bundle zip |
| `url` | `String` | https://central.sonatype.com/api/v1/publisher/upload | Sonatype Publish Portal API [upload endpoint URL](https://central.sonatype.com/api-doc) |
| `bundleName` | `String` |  | Optional deployment/bundle name, if not present Sonatype will use bundle file name |
| `autoPublish` | `Boolean` | `false` | `true` to automatically proceed to publish to Maven Central, `false` to publish via the Portal UI |
| `stagingDir` | `Directory` | `$dir/staging` | **(readonly)** Output directory for storing publication artifacts |

## Reacting to the java plugin
In order to enable javadoc and sources jars plugin invokes `withJavadocJar()` and `withSourcesJar()` on java extension when `java` plugin is applied to a project.

## Plugins autoconfiguration
Following plugins automatically applied and configured:
- [maven-publish](https://docs.gradle.org/current/userguide/publishing_maven.html) configured with `sonatypeStaging` maven repo to publish artifacts into local **staging** directory.
- [signing](https://docs.gradle.org/current/userguide/signing_plugin.html) configured for signing all maven publicatons registered on publishing extension, `sonatypeSigningKey` and `sonatypeSigningSecret` project properties if available used to configure `useInMemoryPgpKeys`.
