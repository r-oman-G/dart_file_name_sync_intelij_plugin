import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.refactoring.rename.RenameProcessor

class SyncClassNameWithDartFileAction : AnAction() {

    fun camelToSnake(str: String): String {
        val regex = "(?<=[a-z])(?=[A-Z])".toRegex()
        return regex.replace(str, "_").lowercase()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val currentFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull() ?: return
        val psiFile = PsiManager.getInstance(project).findFile(currentFile) ?: return

        val dataContext = createDataContext(psiFile, project)
        val selectedText = getSelectedTextOrUnderCaret(e);
        val camelToSnake = camelToSnake(selectedText)
        val newName = "$camelToSnake.dart"

        WriteCommandAction.runWriteCommandAction(project) {
            if (psiFile.name == newName) {
                Messages.showErrorDialog(
                    project,
                    "The file name is already $newName",
                    "Error Creating File"
                )
                return@runWriteCommandAction
            } else if (newName.isBlank()) {
                Messages.showErrorDialog(
                    project,
                    "A file with this name already exists in the current directory.",
                    "Error Creating File"
                )
                return@runWriteCommandAction
            }


            RenameProcessor(project, psiFile, newName, false, true).run()
        }
    }

    private fun createDataContext(psiFile: PsiElement, project: Project): DataContext {
        return DataContext { dataId ->
            when (dataId) {
                CommonDataKeys.PSI_ELEMENT.name -> psiFile
                CommonDataKeys.PROJECT.name -> project
                else -> null
            }
        }
    }


    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val project = e.project ?: return
        val currentFile = e.getData(CommonDataKeys.PSI_FILE) ?: return

        // Generate the default file name, appending ".new" if the original file has no extension
        val originalFileName = currentFile.name
        val isDartFile = originalFileName.endsWith(".dart")

        val selectedText = getSelectedTextOrUnderCaret(e);

        e.presentation.isEnabledAndVisible = selectedText.isNotEmpty() && isDartFile && selectedText.isNotEmpty()
    }

    fun getSelectedTextOrUnderCaret(e: AnActionEvent): String {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return ""
        val selectionModel = editor.selectionModel
        val selectedText = selectionModel.selectedText

        // If no text is selected, get the word under the caret
        val caretModel = editor.caretModel
        val offset = caretModel.offset
        val document = editor.document
        val lineStartOffset = document.getLineStartOffset(document.getLineNumber(offset))
        val lineEndOffset = document.getLineEndOffset(document.getLineNumber(offset))
        val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))
        val wordStart = lineText.lastIndexOf(' ', offset - lineStartOffset) + 1
        val wordEnd = lineText.indexOf(' ', offset - lineStartOffset)
        val wordUnderCarret =
            if (wordEnd == -1) lineText.substring(wordStart) else lineText.substring(wordStart, wordEnd)

        return selectedText ?: wordUnderCarret
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}


