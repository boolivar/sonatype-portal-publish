package org.bool.sonatype.gradle;

import org.apache.commons.lang3.tuple.Pair;
import org.gradle.api.Project;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.plugins.signing.SigningPlugin;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.io.File;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockServerExtension.class)
class SonatypePortalPublishPluginTest {

    private static final String PATH = "/test/upload";

    @TempDir
    private File tmpDir;

    @Test
    void testPlugin() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("io.github.boolivar.sonatype-portal-publish");

        assertThat(project.getExtensions().getExtraProperties().get("SonatypePortalPublishTask"))
            .isEqualTo(SonatypePortalPublishTask.class);

        assertThat(project.getExtensions().getExtensionsSchema())
            .extracting(s -> Pair.of(s.getName(), s.getPublicType().getConcreteClass()))
            .anyMatch(ext -> "sonatypePublish".equals(ext.getKey()) && SonatypePortalPublishExtension.class.isAssignableFrom(ext.getValue()));

        assertThat(project.getPlugins())
            .anyMatch(plugin -> MavenPublishPlugin.class.isInstance(plugin))
            .anyMatch(plugin -> SigningPlugin.class.isInstance(plugin))
            ;

        assertThat(project.getTasks())
            .anyMatch(task -> "publishToSonatype".equals(task.getName()) && SonatypePortalPublishTask.class.isInstance(task))
            .anyMatch(task -> "sonatypeStagingZip".equals(task.getName()) && Zip.class.isInstance(task))
            ;
    }

    @Test
    void testPublishTask(ClientAndServer server) throws Exception {
        server.when(HttpRequest.request(PATH)
                .withHeader("Authorization", "Bearer <username:password>")
                .withHeader("Content-Type", "multipart/form-data; boundary=abcdef")
            ).respond(HttpResponse.response("OK").withStatusCode(200));

        File bundleFile = new File(tmpDir, "bundle.file");
        Files.write(bundleFile.toPath(), "test bundle".getBytes());

        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("io.github.boolivar.sonatype-portal-publish");

        SonatypePortalPublishTask task = project.getTasks().create("testTask", SonatypePortalPublishTask.class);
        task.getUrl().set("http://localhost:" + server.getPort() + PATH);
        task.getToken().set("<username:password>");
        task.getBundle().set(bundleFile);

        task.publish("abcdef");

        server.verify(HttpRequest.request(PATH)
            .withBody("--abcdef\r\nContent-Disposition: form-data; name=\"bundle\"; filename=\"bundle.file\"\r\nContent-Type: application/octet-stream\r\nContent-Length: 11\r\n\r\ntest bundle\r\n--abcdef--\r\n"));
    }
}
