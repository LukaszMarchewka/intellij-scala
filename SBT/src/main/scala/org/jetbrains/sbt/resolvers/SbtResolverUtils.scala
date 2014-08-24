package org.jetbrains.sbt
package resolvers

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.libraries.{LibraryTablesRegistrar, Library}
import com.intellij.psi.PsiFile
import org.apache.maven.index.ArtifactInfo
import org.jetbrains.sbt.project.module.SbtModule

/**
 * @author Nikolay Obedin
 * @since 8/4/14.
 */
object SbtResolverUtils {

  def getProjectLibraries(fileOpt: Option[PsiFile]): Seq[Library] = fileOpt match {
    case Some(file) if file.getProject != null =>
      LibraryTablesRegistrar.getInstance().getLibraryTable(file.getProject).getLibraries
    case _ => Seq.empty
  }

  def getProjectResolvers(fileOpt: Option[PsiFile]): Seq[SbtResolver] = fileOpt match {
    case Some(file) =>
      val moduleManager = ModuleManager.getInstance(file.getProject)
      if (moduleManager == null) return Seq.empty
      moduleManager.getModules.toSeq.flatMap(SbtModule.getResolversFrom)
    case _ => Seq.empty
  }

  def joinGroupArtifact(group: String, artifact: String) = group + ":" + artifact
  def joinGroupArtifact(artifact: ArtifactInfo) = artifact.groupId + ":" + artifact.artifactId
  def joinGroupArtifactVersion(group: String, artifact: String, version: String) = group + ":" + artifact + ":" + version
}
