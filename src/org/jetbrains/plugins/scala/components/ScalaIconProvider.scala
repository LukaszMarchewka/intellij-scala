package org.jetbrains.plugins.scala.components

import com.intellij.psi._
import javax.swing.Icon
import org.jetbrains.plugins.scala.lang.psi.ScalaFile
import com.intellij.ide.IconProvider
import org.jetbrains.annotations.Nullable

class ScalaIconProvider extends IconProvider {
  [Nullable]
  override def getIcon(element : PsiElement, flags : Int) : Icon = {
    if (element.isInstanceOf[ScalaFile]) {
      val file = element.asInstanceOf[ScalaFile]
      val name = file.getVirtualFile.getNameWithoutExtension
      for (val clazz <- file.getTmplDefs) {
        if (name.equals(clazz.getName)) return clazz.getIcon(flags)
      }
    }

    null
  }

  def getComponentName = "Scala Icon Provider"

  def initComponent = {}

  def disposeComponent = {}
}
