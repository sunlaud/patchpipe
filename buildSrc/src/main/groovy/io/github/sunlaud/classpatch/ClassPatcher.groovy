package io.github.sunlaud.classpatch

import com.googlecode.dex2jar.tools.ApkSign
import com.googlecode.dex2jar.tools.Dex2jarCmd
import com.googlecode.dex2jar.tools.Jar2Dex
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip

class ClassPatcher implements Plugin<Project> {
    void apply(Project project) {

        def originalApk = "some.apk"
        def extractedOriginalApkDir = "${project.buildDir}/apk/original/apk-extracted"
        def originalClassesJar = "${project.buildDir}/apk/original/classes.jar"

        def patchedArtifactsDir = "${project.buildDir}/apk/patched"
        def patchedClassesJarName = "modified-classes.jar"
        def patchedApkName = "patched.apk"


        project.task('extractApk', type: Copy) {
                from project.zipTree(originalApk)
                into extractedOriginalApkDir
        }
        project.task('dex2jar', dependsOn: project.extractApk) {
            inputs.file("${extractedOriginalApkDir}/classes.dex")
            outputs.file(originalClassesJar)
            doLast {
                //TODO no exception here in case of error (caught inside cli util)
                new Dex2jarCmd().doMain(['--force', '--output', originalClassesJar, "${extractedOriginalApkDir}/classes.dex"] as String[])
            }
        }
        project.task('patchClasses', dependsOn: [project.classes, project.dex2jar]) {
            inputs.file project.dex2jar.
            outputs.files("${patchedArtifactsDir}/modified-classes")
//            main = 'Patch'
//            classpath = sourceSets.main.runtimeClasspath + files(originalClassesJar)
//            args "${patchedArtifactsDir}/modified-classes"
        }
        project.task('createPatchedClassesJar', dependsOn: [project.patchClasses], type: Zip) {
            from project.patchClasses
            from project.zipTree(originalClassesJar)
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            destinationDir = project.file(patchedArtifactsDir)
            archiveName = patchedClassesJarName
        }

        project.task('jar2dex', dependsOn: project.createPatchedClassesJar) {
            inputs.file project.createPatchedClassesJar.archivePath
            outputs.file("${patchedArtifactsDir}/classes.dex")
            doLast {
                new Jar2Dex().doMain(['--force', '--output', "${patchedArtifactsDir}/classes.dex", "${patchedArtifactsDir}/${patchedClassesJarName}"] as String[])
            }
        }
        project.task('packApk', dependsOn: project.jar2dex, type: Zip) {
            from project.jar2dex
            from project.extractApk
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            destinationDir = project.file(patchedArtifactsDir)
            archiveName = patchedApkName
        }
        project.task('signApk', dependsOn: project.packApk) {
            inputs.file project.packApk.archivePath
            outputs.file "${patchedArtifactsDir}/signed-${patchedApkName}"
            doLast {
                new ApkSign().doMain(['--force', '--output', "${patchedArtifactsDir}/signed-${patchedApkName}", project.packApk.archivePath] as String[])
            }
        }

        project.defaultTasks('clean', 'signApk')
    }
}
