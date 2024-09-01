package org.bool.sonatype.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class SonatypePortalPublishPlugin implements Plugin<Project> {

    public void apply(Project project) {
        project.getExtensions().getExtraProperties().set("SonatypePortalPublishTask", SonatypePortalPublishTask.class);
    }
}
