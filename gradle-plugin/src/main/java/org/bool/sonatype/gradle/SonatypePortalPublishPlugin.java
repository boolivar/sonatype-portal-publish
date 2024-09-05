package org.bool.sonatype.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.publish.plugins.PublishingPlugin;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.plugins.signing.SigningExtension;
import org.gradle.plugins.signing.SigningPlugin;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SonatypePortalPublishPlugin implements Plugin<Project> {

    public static final String MAVEN_CENTRAL_USER_PROPERTY = "sonatypeMavenCentralUser";

    public static final String MAVEN_CENTRAL_PASSWORD_PROPERTY = "sonatypeMavenCentralPassword";

    public static final String SIGNING_KEY_PROPERTY = "sonatypeSigningKey";

    public static final String SIGNING_SECRET_PROPERTY = "sonatypeSigningSecret";

    public static final String SONATYPE_STAGING_TASK = "publishMavenPublicationToSonatypeStagingRepository";

    public static final String SONATYPE_ZIP_TASK = "sonatypeStagingZip";

    public static final String SONATYPE_PUBLISH_TASK = "publishToSonatype";

    public static final String SONATYPE_STAGING_REPO = "sonatypeStaging";

    private static final String OUTPUT_DIR = "sonatypePublish";

    public void apply(Project project) {
        project.getPlugins().apply(MavenPublishPlugin.class);
        project.getPlugins().apply(SigningPlugin.class);

        ExtensionContainer extensions = project.getExtensions();
        extensions.getExtraProperties().set(SonatypePortalPublishTask.class.getSimpleName(), SonatypePortalPublishTask.class);
        SonatypePortalPublishExtension sonatypePublish = extensions.create(SonatypePortalPublishExtension.NAME, SonatypePortalPublishExtension.class);
        sonatypePublish.getDir().convention(project.getLayout().getBuildDirectory().dir(OUTPUT_DIR));

        project.getPlugins().withType(JavaPlugin.class, javaPlugin -> {
            JavaPluginExtension java = extensions.getByType(JavaPluginExtension.class);
            java.withJavadocJar();
            java.withSourcesJar();
        });

        extensions.getByType(PublishingExtension.class).getRepositories().maven(repository -> {
            repository.setName(SONATYPE_STAGING_REPO);
            repository.setUrl(sonatypePublish.getStagingDir());
        });

        project.getTasks().register(SONATYPE_ZIP_TASK, Zip.class, zip -> {
            zip.dependsOn(SONATYPE_STAGING_TASK);
            zip.from(sonatypePublish.getStagingDir());
            zip.exclude("**/maven-metadata.*");
            zip.getArchiveBaseName().set(project.getName());
            zip.getDestinationDirectory().set(sonatypePublish.getDir());
        });

        project.getTasks().register(SONATYPE_PUBLISH_TASK, SonatypePortalPublishTask.class, publish -> {
            publish.setGroup(PublishingPlugin.PUBLISH_TASK_GROUP);
            publish.setDescription("Publishes Maven publication to Sonatype Maven Central using Portal Publisher API");
            publish.getUrl().set(sonatypePublish.getUrl());
            publish.getAutoPublish().set(sonatypePublish.getAutoPublish());
            publish.getBundle().set(project.getTasks().named(SONATYPE_ZIP_TASK, Zip.class).get().getArchiveFile());
            publish.getBundleName().set(sonatypePublish.getBundleName());
            publish.getToken().set(project.provider(() -> encodeToken(project)));
        });

        project.afterEvaluate(prj -> {
            SigningExtension signing = prj.getExtensions().getByType(SigningExtension.class);
            signing.sign(prj.getExtensions().getByType(PublishingExtension.class).getPublications().matching(MavenPublication.class::isInstance));
            if (prj.findProperty(SIGNING_KEY_PROPERTY) != null) {
                signing.useInMemoryPgpKeys((String) prj.property(SIGNING_KEY_PROPERTY), (String) prj.property(SIGNING_SECRET_PROPERTY));
            }
        });
    }

    private String encodeToken(Project project) {
        String creds = project.property(MAVEN_CENTRAL_USER_PROPERTY) + ":" + project.property(MAVEN_CENTRAL_PASSWORD_PROPERTY);
        return Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
    }
}
