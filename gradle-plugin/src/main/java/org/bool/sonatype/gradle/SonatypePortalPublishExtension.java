package org.bool.sonatype.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

public interface SonatypePortalPublishExtension {

    String NAME = "sonatypePublish";

    DirectoryProperty getDir();

    Property<String> getUrl();

    Property<Boolean> getAutoPublish();
}
