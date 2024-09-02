package org.bool.sonatype.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.publish.plugins.PublishingPlugin;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.plugins.signing.SigningExtension;
import org.gradle.plugins.signing.SigningPlugin;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SonatypePortalPublishPlugin implements Plugin<Project> {

    public static final String MAVEN_CENTRAL_USER_PROPERTY = "sonatype.mavenCentralUser";

    public static final String MAVEN_CENTRAL_PASSWORD_PROPERTY = "sonatype.mavenCentralPassword";

    public static final String SIGNING_SECRET_PROPERTY = "sonatype.signingSecret";

    public static final String SIGNING_KEY_PROPERTY = "sonatype.signingKey";

    public static final String SONATYPE_STAGING_TASK = "publishMavenPublicationToSonatypeStagingRepository";

    public static final String SONATYPE_ZIP_TASK = "sonatypeStagingZip";

    public static final String SONATYPE_PUBLISH_TASK = "publishToSonatype";

    private static final String SONATYPE_STAGING_REPO = "sonatypeStaging";

    private static final String OUTPUT_DIR = "sonatypePublish";

    private static final String STAGING_DIR = "staging";

    public void apply(Project project) {
        ExtensionContainer extensions = project.getExtensions();
        extensions.getExtraProperties().set(SonatypePortalPublishTask.class.getSimpleName(), SonatypePortalPublishTask.class);
        SonatypePortalPublishExtension extension = extensions.create(SonatypePortalPublishExtension.NAME, SonatypePortalPublishExtension.class);
        extension.getDir().convention(project.getLayout().getBuildDirectory().dir(OUTPUT_DIR));

        project.getPlugins().withType(MavenPublishPlugin.class, mavenPublish -> {
            project.getPlugins().apply(SigningPlugin.class);

            PublishingExtension publishing = extensions.findByType(PublishingExtension.class);
            publishing.getRepositories().maven(repository -> {
                repository.setName(SONATYPE_STAGING_REPO);
                repository.setUrl(extension.getDir().dir(STAGING_DIR));
            });

            extensions.configure(SigningExtension.class, signing -> {
                signing.sign(publishing.getPublications());
                if (project.findProperty(SIGNING_KEY_PROPERTY) != null) {
                    signing.useInMemoryPgpKeys((String) project.property(SIGNING_KEY_PROPERTY), (String) project.property(SIGNING_SECRET_PROPERTY));
                }
            });

            TaskProvider<Zip> zipTask = project.getTasks().register(SONATYPE_ZIP_TASK, Zip.class, zip -> {
                zip.dependsOn(SONATYPE_STAGING_TASK);
                zip.from(extension.getDir().dir(STAGING_DIR));
                zip.exclude("**/maven-metadata.*");
                zip.getArchiveBaseName().set(project.getName());
                zip.getDestinationDirectory().set(extension.getDir());
            });

            project.getTasks().register(SONATYPE_PUBLISH_TASK, SonatypePortalPublishTask.class, publish -> {
                publish.setGroup(PublishingPlugin.PUBLISH_TASK_GROUP);
                publish.setDescription("Publishes Maven publication to Sonatype Maven Central using Portal Publisher API");
                publish.getUrl().set(extension.getUrl());
                publish.getAutoPublish().set(extension.getAutoPublish());
                publish.getBundle().set(zipTask.get().getArchiveFile());
                publish.getToken().set(project.provider(() -> encodeToken(project)));
            });
        });
    }

    private String encodeToken(Project project) {
        String creds = project.property(MAVEN_CENTRAL_USER_PROPERTY) + ":" + project.property(MAVEN_CENTRAL_PASSWORD_PROPERTY);
        return Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
    }
}
