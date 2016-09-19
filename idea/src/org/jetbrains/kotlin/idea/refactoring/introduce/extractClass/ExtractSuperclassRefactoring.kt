/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.refactoring.introduce.extractClass

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.extractSuperclass.ExtractSuperClassUtil
import com.intellij.refactoring.memberPullUp.PullUpProcessor
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.refactoring.util.DocCommentPolicy
import com.intellij.refactoring.util.MoveRenameUsageInfo
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.actions.NewKotlinFileAction
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.refactoring.introduce.insertDeclaration
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberInfo
import org.jetbrains.kotlin.idea.refactoring.memberInfo.getChildrenToAnalyze
import org.jetbrains.kotlin.idea.refactoring.memberInfo.toJavaMemberInfo
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.KotlinMoveTargetForDeferredFile
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.KotlinMoveTargetForExistingElement
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.MoveConflictChecker
import org.jetbrains.kotlin.idea.refactoring.runSynchronouslyWithProgress
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import java.util.*

data class ExtractSuperclassInfo(
        val originalClass: KtClassOrObject,
        val memberInfos: Collection<KotlinMemberInfo>,
        val targetParent: PsiElement,
        val targetFileName: String,
        val newClassName: String,
        val docPolicy: DocCommentPolicy<*>
)

class ExtractSuperclassRefactoring(
        private var extractInfo: ExtractSuperclassInfo
) {
    companion object {
        private fun getElementsToMove(
                memberInfos: Collection<KotlinMemberInfo>,
                originalClass: KtClassOrObject
        ): Map<KtElement, KotlinMemberInfo?> {
            val project = originalClass.project
            val elementsToMove = LinkedHashMap<KtElement, KotlinMemberInfo?>()
            runReadAction {
                val superInterfacesToMove = ArrayList<KtElement>()
                for (memberInfo in memberInfos) {
                    val member = memberInfo.member ?: continue
                    if (memberInfo.isSuperClass) {
                        superInterfacesToMove += member
                    }
                    else {
                        elementsToMove[member] = memberInfo
                    }
                }

                val superTypeList = originalClass.getSuperTypeList()
                if (superTypeList != null) {
                    for (superTypeListEntry in originalClass.getSuperTypeListEntries()) {
                        val superType = superTypeListEntry.analyze(BodyResolveMode.PARTIAL)[BindingContext.TYPE, superTypeListEntry.typeReference]
                                        ?: continue
                        val superClassDescriptor = superType.constructor.declarationDescriptor ?: continue
                        val superClass = DescriptorToSourceUtilsIde.getAnyDeclaration(project, superClassDescriptor) as? KtClass ?: continue
                        if (!superClass.isInterface() || superClass in superInterfacesToMove) {
                            elementsToMove[superTypeListEntry] = null
                        }
                    }
                }
            }
            return elementsToMove
        }

        fun collectConflicts(
                originalClass: KtClassOrObject,
                memberInfos: List<KotlinMemberInfo>,
                targetParent: PsiElement
        ): MultiMap<PsiElement, String> {
            val conflicts = MultiMap<PsiElement, String>()

            val project = originalClass.project

            val elementsToMove = getElementsToMove(memberInfos, originalClass).keys

            val moveTarget = if (targetParent is PsiDirectory) {
                val targetPackage = targetParent.getPackage() ?: return conflicts
                KotlinMoveTargetForDeferredFile(FqName(targetPackage.qualifiedName), targetParent) { null }
            }
            else {
                KotlinMoveTargetForExistingElement(targetParent as KtElement)
            }
            val conflictChecker = MoveConflictChecker(project, elementsToMove, moveTarget, originalClass)

            project.runSynchronouslyWithProgress(RefactoringBundle.message("detecting.possible.conflicts"), true) {
                runReadAction {
                    val usages = ArrayList<UsageInfo>()
                    for (element in elementsToMove) {
                        ReferencesSearch.search(element).mapTo(usages) { MoveRenameUsageInfo(it, element) }
                        if (element is KtCallableDeclaration) {
                            element.toLightMethods().flatMapTo(usages) {
                                MethodReferencesSearch.search(it).map { MoveRenameUsageInfo(it, element) }
                            }
                        }
                    }
                    conflictChecker.checkAllConflicts(usages, conflicts)
                    if (targetParent is PsiDirectory) {
                        ExtractSuperClassUtil.checkSuperAccessible(targetParent, conflicts, originalClass.toLightClass())
                    }
                }
            }

            return conflicts
        }
    }

    private val project = extractInfo.originalClass.project
    private val psiFactory = KtPsiFactory(project)
    private val typeParameters = LinkedHashSet<KtTypeParameter>()

    private val bindingContext = extractInfo.originalClass.analyze(BodyResolveMode.PARTIAL)

    private fun collectTypeParameters(refTarget: PsiElement?) {
        if (refTarget is KtTypeParameter && refTarget.getStrictParentOfType<KtTypeParameterListOwner>() == extractInfo.originalClass) {
            typeParameters += refTarget
            refTarget.accept(
                    object : KtTreeVisitorVoid() {
                        override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                            (expression.mainReference.resolve() as? KtTypeParameter)?.let { typeParameters += it }
                        }
                    }
            )
        }
    }

    private fun analyzeContext() {
        val visitor = object : KtTreeVisitorVoid() {
            override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                val refTarget = expression.mainReference.resolve()
                collectTypeParameters(refTarget)
            }
        }
        getElementsToMove(extractInfo.memberInfos, extractInfo.originalClass)
                .asSequence()
                .flatMap {
                    val (element, info) = it
                    if (info != null) info.getChildrenToAnalyze().asSequence() else sequenceOf(element)
                }
                .forEach { it.accept(visitor) }
    }

    private fun createClass(superClassEntry: KtSuperTypeListEntry?): KtClass {
        val targetParent = extractInfo.targetParent
        val newClassName = extractInfo.newClassName
        val originalClass = extractInfo.originalClass

        val newClass = if (targetParent is PsiDirectory) {
            val template = FileTemplateManager.getInstance(project).getInternalTemplate("Kotlin File")
            val newFile = NewKotlinFileAction.createFileFromTemplate(extractInfo.targetFileName, template, targetParent) as KtFile
            newFile.add(psiFactory.createClass("class $newClassName")) as KtClass
        }
        else {
            val targetSibling = originalClass.parentsWithSelf.first { it.parent == targetParent }
            insertDeclaration(psiFactory.createClass("class $newClassName {\n\n}"), targetSibling)
        }

        val shouldBeAbstract = extractInfo.memberInfos.any { it.isToAbstract }
        newClass.addModifier(if (shouldBeAbstract) KtTokens.ABSTRACT_KEYWORD else KtTokens.OPEN_KEYWORD)

        if (typeParameters.isNotEmpty()) {
            val typeParameterListText = typeParameters.sortedBy { it.startOffset }.map { it.text }.joinToString(prefix = "<", postfix = ">")
            newClass.addAfter(psiFactory.createTypeParameterList(typeParameterListText), newClass.nameIdentifier)
        }

        val targetPackageFqName = (targetParent as? PsiDirectory)?.getPackage()?.qualifiedName

        val superTypeText = buildString {
            if (!targetPackageFqName.isNullOrEmpty()) {
                append(targetPackageFqName).append('.')
            }
            append(newClassName)
            if (typeParameters.isNotEmpty()) {
                append(typeParameters.sortedBy { it.startOffset }.map { it.name }.joinToString(prefix = "<", postfix = ">"))
            }
        }
        val needSuperCall = superClassEntry is KtSuperTypeCallEntry
                            || originalClass.hasPrimaryConstructor()
                            || originalClass.getSecondaryConstructors().isEmpty()
        val newSuperTypeCallEntry = if (needSuperCall) {
            psiFactory.createSuperTypeCallEntry("$superTypeText()")
        }
        else {
            psiFactory.createSuperTypeEntry(superTypeText)
        }
        if (superClassEntry != null) {
            val qualifiedTypeRefText = bindingContext[BindingContext.TYPE, superClassEntry.typeReference]?.let {
                IdeDescriptorRenderers.SOURCE_CODE.renderType(it)
            }
            val superClassEntryToAdd = if (qualifiedTypeRefText != null) {
                superClassEntry.copied().apply { typeReference?.replace(psiFactory.createType(qualifiedTypeRefText)) }
            }
            else superClassEntry
            newClass.addSuperTypeListEntry(superClassEntryToAdd)
            ShortenReferences.DEFAULT.process(superClassEntry.replaced(newSuperTypeCallEntry))
        }
        else {
            ShortenReferences.DEFAULT.process(originalClass.addSuperTypeListEntry(newSuperTypeCallEntry))
        }

        ShortenReferences.DEFAULT.process(newClass)

        return newClass
    }

    fun performRefactoring() {
        val originalClass = extractInfo.originalClass

        KotlinExtractSuperclassHandler.getErrorMessage(originalClass)?.let {
            throw CommonRefactoringUtil.RefactoringErrorHintException(it)
        }

        val originalClassDescriptor = originalClass.resolveToDescriptor() as ClassDescriptor
        val superClassDescriptor = originalClassDescriptor.getSuperClassNotAny()
        val superClassEntry = originalClass.getSuperTypeListEntries().firstOrNull {
            bindingContext[BindingContext.TYPE, it.typeReference]?.constructor?.declarationDescriptor == superClassDescriptor
        }

        project.runSynchronouslyWithProgress(RefactoringBundle.message("progress.text"), true) { runReadAction { analyzeContext() } }

        project.executeWriteCommand(KotlinExtractSuperclassHandler.REFACTORING_NAME) {
            val newClass = createClass(superClassEntry)

            val subClass = extractInfo.originalClass.toLightClass()
            val superClass = newClass.toLightClass()

            PullUpProcessor(
                    subClass,
                    superClass ?: return@executeWriteCommand,
                    extractInfo.memberInfos.mapNotNull { it.toJavaMemberInfo() }.toTypedArray(),
                    extractInfo.docPolicy
            ).run()
        }
    }
}
