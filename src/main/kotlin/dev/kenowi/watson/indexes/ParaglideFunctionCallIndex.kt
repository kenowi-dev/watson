package dev.kenowi.watson.indexes

import com.intellij.lang.javascript.JavaScriptFileType
import com.intellij.lang.javascript.TypeScriptFileType
import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import dev.blachut.svelte.lang.SvelteHtmlFileType
import dev.kenowi.watson.utils.MessageUtils
import java.io.DataInput
import java.io.DataOutput
import java.util.Collections


/**
 * Index that maps message function names to their call locations.
 * Key: function name (e.g., "hello_world")
 * Value: list of offsets where this function is called in the file
 */
class ParaglideFunctionCallIndex : FileBasedIndexExtension<String, List<Int>>() {

    companion object {
        val NAME = ID.create<String, List<Int>>("ParaglideFunctionCallIndex")
    }

    override fun getName(): ID<String, List<Int>> = NAME

    override fun getIndexer(): DataIndexer<String, List<Int>, FileContent> {

        return DataIndexer { inputData ->
            val project = inputData.project
            val file: VirtualFile = inputData.file

            if (ProjectFileIndex.getInstance(project).isExcluded(file)) {
                return@DataIndexer emptyMap<String, MutableList<Int>>()
            }
            val result = mutableMapOf<String, MutableList<Int>>()

            val psiFile = inputData.psiFile

            psiFile.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    super.visitElement(element)

                    if (element is JSCallExpression) {
                        val methodExpr = element.methodExpression
                        if (methodExpr is JSReferenceExpression) {
                            val functionName = methodExpr.referenceName
                            if (functionName != null && MessageUtils.isMessageCall(element)) {
                                result.getOrPut(functionName) { mutableListOf() }
                                    .add(element.textRange.startOffset)
                            }
                        }
                    }
                }
            })

            result
        }
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> {
        return EnumeratorStringDescriptor.INSTANCE
    }

    private val externalizer = object : DataExternalizer<List<Int>> {
        override fun save(out: DataOutput, value: List<Int>) {
            out.writeInt(value.size)
            value.forEach(out::writeInt)
        }

        override fun read(input: DataInput): List<Int> {
            val size = input.readInt()
            return List(size) { input.readInt() }
        }
    }

    override fun getValueExternalizer(): DataExternalizer<List<Int>> = externalizer

    override fun getVersion(): Int = 2

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return object : DefaultFileTypeSpecificInputFilter(
            JavaScriptFileType,
            TypeScriptFileType,
            SvelteHtmlFileType
        ) { }
    }

    override fun dependsOnFileContent(): Boolean = true
}