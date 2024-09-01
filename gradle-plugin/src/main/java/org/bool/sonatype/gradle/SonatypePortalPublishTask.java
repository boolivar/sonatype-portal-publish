package org.bool.sonatype.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.AbstractMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class SonatypePortalPublishTask extends DefaultTask {

    public static final String SONATYPE_URL = "https://central.sonatype.com/api/v1/publisher/upload";

    private static final String CRLF = "\r\n";

    @Optional
    @Input
    public abstract Property<String> getBundleName();

    @Optional
    @Input
    public abstract Property<Boolean> getAutoPublish();

    @InputFile
    public abstract RegularFileProperty getBundle();

    @Optional
    @Input
    public abstract Property<String> getUrl();

    @Input
    public abstract Property<String> getToken();

    @TaskAction
    public void publish() throws IOException, URISyntaxException {
        publish(UUID.randomUUID().toString());
    }

    void publish(String boundary) throws IOException, URISyntaxException {
        File file = getBundle().get().getAsFile();
        String headers = headers(file);
        HttpURLConnection connection = (HttpURLConnection) uri().toURL().openConnection();
        try {
            connection.setRequestProperty("Authorization", "Bearer " + getToken().get());
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setDoOutput(true);
            try (BufferedOutputStream out = new BufferedOutputStream(connection.getOutputStream())) {
                write("--" + boundary + CRLF, out);
                write(headers, out);
                Files.copy(file.toPath(), out);
                write(CRLF + "--" + boundary + "--" + CRLF, out);
            }
            if ((connection.getResponseCode() / 100) != 2) {
                throw new IOException("Request error: " + connection.getResponseCode() + " " + connection.getResponseMessage());
            }
        } finally {
            connection.disconnect();
        }
    }

    private URI uri() throws URISyntaxException {
        URI baseUri = URI.create(getUrl().getOrElse(SONATYPE_URL));
        String publishingType = "publishingType=" + (getAutoPublish().getOrElse(false) ? "AUTOMATIC" : "USER_MANAGED");
        String queryParams = publishingType + getBundleName().map(name -> "&name=" + name).getOrElse("");
        return new URI(baseUri.getScheme(), baseUri.getAuthority(), baseUri.getPath(), queryParams, null);
    }

    private String headers(File file) throws IOException {
        return Stream.of(
            new AbstractMap.SimpleEntry<>("Content-Disposition", "form-data; name=\"bundle\"; filename=\"" + file.getName() + '"'),
            new AbstractMap.SimpleEntry<>("Content-Type", "application/octet-stream"),
            new AbstractMap.SimpleEntry<>("Content-Length", Files.size(file.toPath()))
        )
        .map(e -> e.getKey() + ": " + e.getValue() + CRLF)
        .collect(Collectors.joining("", "", CRLF));
    }

    private void write(String string, OutputStream out) throws IOException {
        out.write(string.getBytes(StandardCharsets.UTF_8));
    }
}
