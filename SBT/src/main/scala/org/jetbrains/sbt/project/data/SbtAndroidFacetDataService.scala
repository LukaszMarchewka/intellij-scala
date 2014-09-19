package org.jetbrains.sbt
package project.data


import java.io.{File, OutputStreamWriter}
import java.util

import com.intellij.facet.FacetManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.{DataNode, ProjectKeys}
import com.intellij.openapi.externalSystem.service.project.{PlatformFacade, ProjectStructureHelper}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.SystemProperties
import org.jetbrains.android.facet.{AndroidFacet, AndroidFacetType, AndroidRootUtil}

import scala.collection.JavaConverters._

/**
 * @author Nikolay Obedin
 * @since 8/12/14.
 */
class SbtAndroidFacetDataService(platformFacade: PlatformFacade, helper: ProjectStructureHelper)
        extends AbstractDataService[AndroidFacetData, AndroidFacet](AndroidFacetData.Key) {

  def doImportData(toImport: util.Collection[DataNode[AndroidFacetData]], project: Project) {
    toImport.asScala.foreach { facetNode =>
      val module = {
        val moduleData: ModuleData = facetNode.getData(ProjectKeys.MODULE)
        helper.findIdeModule(moduleData.getExternalName, project)
      }

      val facet = Option(FacetManager.getInstance(module)).flatMap { manager =>
        Option(manager.getFacetByType(AndroidFacet.ID))
      }

      def configure(facet: AndroidFacet) = {
        val data = facetNode.getData
        var proguardFilePath: String = ""
        if (data.proguardConfig.nonEmpty)
          ApplicationManager.getApplication.runWriteAction(new Runnable{
            def run(): Unit = {
              val proguardFile = project.getBaseDir.createChildData(this, "proguard-sbt.txt")
              if (proguardFile.isValid && proguardFile.isWritable) {
                proguardFile.setBOM(null)
                using(new OutputStreamWriter(proguardFile.getOutputStream(this))) { output =>
                  output.write(data.proguardConfig.mkString(SystemProperties.getLineSeparator))
                }
                proguardFilePath = proguardFile.getCanonicalPath
              }
            }
          })

        val props = facet.getConfiguration.getState
        val base = AndroidRootUtil.getModuleDirPath(module)
        def getRelativePath(f: File) = "/" + FileUtil.getRelativePath(base, f.getAbsolutePath, File.separatorChar)
        props.GEN_FOLDER_RELATIVE_PATH_APT = getRelativePath(data.gen)
        props.GEN_FOLDER_RELATIVE_PATH_AIDL = getRelativePath(data.gen)
        props.MANIFEST_FILE_RELATIVE_PATH = getRelativePath(data.manifest)
        props.RES_FOLDER_RELATIVE_PATH = getRelativePath(data.res)
        props.ASSETS_FOLDER_RELATIVE_PATH = getRelativePath(data.assets)
        props.LIBS_FOLDER_RELATIVE_PATH = getRelativePath(data.libs)
        props.APK_PATH = getRelativePath(data.apk)
        props.LIBRARY_PROJECT = data.isLibrary
        props.RUN_PROGUARD = true
        props.myProGuardCfgFiles = new util.ArrayList[String]()
        if (proguardFilePath.nonEmpty)
          props.myProGuardCfgFiles.add(proguardFilePath)
      }

      def createFacet() {
        val facetManager = FacetManager.getInstance(module)
        val model = facetManager.createModifiableModel
        val facet = facetManager.createFacet(new AndroidFacetType, "Android", null)
        configure(facet)
        model.addFacet(facet)
        model.commit()
      }

      facet map configure getOrElse createFacet
    }
  }

  def doRemoveData(toRemove: util.Collection[_ <: AndroidFacet], project: Project) {
    toRemove.asScala.foreach { facet =>
      val facetManager = FacetManager.getInstance(facet.getModule)
      val model = facetManager.createModifiableModel
      model.removeFacet(facet)
      model.commit()
    }
  }
}

