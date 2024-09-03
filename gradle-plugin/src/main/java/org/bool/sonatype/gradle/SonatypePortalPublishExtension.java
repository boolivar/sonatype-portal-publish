package org.bool.sonatype.gradle;

import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

public interface SonatypePortalPublishExtension {

    String NAME = "sonatypePublish";

    DirectoryProperty getDir();

    Property<String> getUrl();

    Property<String> getBundleName();

    Property<Boolean> getAutoPublish();

    default Provider<Directory> getStagingDir() {
        return getDir().dir("staging");
    }
}
