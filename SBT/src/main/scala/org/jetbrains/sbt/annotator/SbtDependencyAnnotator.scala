package org.jetbrains.sbt
package annotator

import com.intellij.lang.annotation.{AnnotationHolder, Annotator}
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.api.base.ScLiteral
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScInfixExpr, ScReferenceExpression}
import org.jetbrains.sbt.annotator.quickfix.{SbtRefreshProjectQuickFix, SbtUpdateResolverIndexesQuickFix}
import org.jetbrains.sbt.resolvers.{SbtResolverIndexesManager, SbtResolverUtils}

/**
 * @author Nikolay Obedin
 * @since 8/4/14.
 */
class SbtDependencyAnnotator extends Annotator {

  private case class ArtifactInfo(group: String, artifact: String, version: String)

  def annotate(element: PsiElement, holder: AnnotationHolder) {

    if (ScalaPsiUtil.fileContext(element).getFileType.getName != Sbt.Name) return

    def isValidOp(op: ScReferenceExpression) = op.getText == "%" || op.getText == "%%"

    def extractInfo(from: PsiElement): Option[ArtifactInfo] = from match {
      case expr: ScInfixExpr => (expr.lOp, expr.rOp) match {
        case (lexpr: ScInfixExpr, versionLit: ScLiteral) if versionLit.isString => (lexpr.lOp, lexpr.rOp) match {
          case (groupLit: ScLiteral, artifactLit: ScLiteral) if groupLit.isString && artifactLit.isString =>
            Some(ArtifactInfo(groupLit.getValue.asInstanceOf[String],
                              artifactLit.getValue.asInstanceOf[String],
                              versionLit.getValue.asInstanceOf[String]))
          case _ => None
        }
        case _ => None
      }
      case _ => None
    }

    def doAnnotate(info: Option[ArtifactInfo]): Unit = info match {
      case Some(ArtifactInfo(group, artifact, version)) =>
        val resolversToUse = SbtResolverUtils.getProjectResolvers(Option(ScalaPsiUtil.fileContext(element)))
        val indexManager = SbtResolverIndexesManager()
        val indexes = resolversToUse.flatMap(indexManager.find).toSet
        if (indexes.isEmpty) return

        val isInRepo = indexes.map { index =>
          index.versions(group, artifact).contains(version)
        }.fold(false) { (a,b) => a || b }
        if (!isInRepo) {
          val annotation = holder.createErrorAnnotation(element, SbtBundle("sbt.annotation.unresolvedDependency"))
          annotation.registerFix(new SbtUpdateResolverIndexesQuickFix)
          annotation.registerFix(new SbtRefreshProjectQuickFix)
        }
      case _ => // do nothing
    }

    element match {
      case lit: ScLiteral => lit.getParent match {
        case parentExpr: ScInfixExpr if isValidOp(parentExpr.operation) => parentExpr.lOp match {
          case _: ScLiteral =>
            doAnnotate(extractInfo(parentExpr.getParent))
          case leftExp: ScInfixExpr if isValidOp(leftExp.operation) =>
            doAnnotate(extractInfo(parentExpr))
          case _ => // do nothing
        }
        case _ => // do nothing
      }
      case _ => // do nothing
    }
  }
}
