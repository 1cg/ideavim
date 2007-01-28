package com.maddyhome.idea.vim.group;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2007 Rick Maddy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.helper.DataPackage;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.helper.EditorHelper;
import com.maddyhome.idea.vim.helper.SearchHelper;
import com.maddyhome.idea.vim.helper.StringHelper;

import java.io.File;
import java.util.HashMap;

/**
 *
 */
public class FileGroup extends AbstractActionGroup
{
    public FileGroup()
    {
    }

    public boolean openFile(String filename, DataPackage context)
    {
        logger.debug("openFile(" + filename + ")");
        Project proj = context.getProject(); // API change - don't merge

        VirtualFile found = null;
        if (filename.length() > 2 && filename.charAt(0) == '~' && filename.charAt(1) == File.separatorChar)
        {
            logger.debug("home dir file");
            String homefile = filename.substring(2);
            String dir = System.getProperty("user.home");
            logger.debug("looking for " + homefile + " in " + dir);
            found = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(dir, homefile));
        }
        else
        {
            ProjectRootManager prm = ProjectRootManager.getInstance(proj);
            VirtualFile[] roots = prm.getContentRoots();
            for (int i = 0; i < roots.length; i++)
            {
                logger.debug("root[" + i + "] = " + roots[i].getPath());
                found = findFile(roots[i], filename);
                if (found != null)
                {
                    break;
                }
            }

            if (found == null)
            {
                found = LocalFileSystem.getInstance().findFileByIoFile(new File(filename));
            }
        }

        if (found != null)
        {
            logger.debug("found file: " + found);
            // Can't open a file unless it has a known file type. The next call will return the known type.
            // If unknown, IDEA will prompt the user to pick a type.
            FileType type = FileTypeManager.getInstance().getKnownFileTypeOrAssociate(found);
            if (type != null)
            {
                FileEditorManager fem = FileEditorManager.getInstance(proj);
                fem.openFile(found, true);

                return true;
            }
            else
            {
                // There was no type and user didn't pick one. Don't open the file
                // Return true here because we found the file but the user canceled by not picking a type.
                return true;
            }
        }
        else
        {
            VimPlugin.showMessage("Unable to find " + filename);

            return false;
        }
    }

    private VirtualFile findFile(VirtualFile root, String filename)
    {
        VirtualFile res = root.findFileByRelativePath(filename);
        if (res != null)
        {
            return res;
        }

        VirtualFile[] children = root.getChildren();
        for (int i = 0; i < children.length; i++)
        {
            if (children[i].isDirectory())
            {
                res = findFile(children[i], filename);
                if (res != null)
                {
                    return res;
                }
            }
            else if (children[i].getName().equals(filename))
            {
                return children[i];
            }
        }

        return null;
    }

    /**
     * Close the current editor
     *
     * @param context The data context
     */
    public void closeFile(Editor editor, DataPackage context)
    {
        Project proj = context.getProject();
        FileEditorManager fem = FileEditorManager.getInstance(proj); // API change - don't merge
        //fem.closeFile(fem.getSelectedFile());
        fem.closeFile(EditorData.getVirtualFile(fem.getSelectedTextEditor()));

        /*
        if (fem.getOpenFiles().length == 0)
        {
            exitIdea();
        }
        */
    }

    /**
     * Close all editors except for the current editor
     *
     * @param context The data context
     */
    public void closeAllButCurrent(DataPackage context)
    {
        KeyHandler.executeAction("CloseAllEditorsButCurrent", context);
    }

    /**
     * Close all editors
     *
     * @param context The data context
     */
    public void closeAllFiles(DataPackage context)
    {
        KeyHandler.executeAction("CloseAllEditors", context);
    }

    /**
     * Saves specific file in the project
     *
     * @param context The data context
     */
    public void saveFile(Editor editor, DataPackage context)
    {
        FileDocumentManager.getInstance().saveDocument(editor.getDocument());
    }

    /**
     * Saves all files in the project
     *
     * @param context The data context
     */
    public void saveFiles(DataPackage context)
    {
        FileDocumentManager.getInstance().saveAllDocuments();
    }

    public void closeProject(DataPackage context)
    {
        KeyHandler.executeAction("CloseProject", context);
    }

    public void exitIdea()
    {
        ApplicationManager.getApplication().exit();
    }

    /**
     * Selects then next or previous editor
     *
     * @param count
     * @param context
     */
    public boolean selectFile(int count, DataPackage context)
    {
        Project proj = context.getProject();
        FileEditorManager fem = FileEditorManager.getInstance(proj); // API change - don't merge
        VirtualFile[] editors = fem.getOpenFiles();
        if (count == 99)
        {
            count = editors.length - 1;
        }
        if (count < 0 || count >= editors.length)
        {
            return false;
        }

        fem.openFile(editors[count], true);

        return true;
    }

    /**
     * Selects then next or previous editor
     *
     * @param count
     * @param context
     */
    public void selectNextFile(int count, DataPackage context)
    {
        Project proj = context.getProject();
        FileEditorManager fem = FileEditorManager.getInstance(proj); // API change - don't merge
        VirtualFile[] editors = fem.getOpenFiles();
        VirtualFile current = fem.getSelectedFiles()[0];
        for (int i = 0; i < editors.length; i++)
        {
            if (editors[i].equals(current))
            {
                int pos = (i + (count % editors.length) + editors.length) % editors.length;

                fem.openFile(editors[pos], true);
            }
        }
    }

    /**
     * Selects previous editor tab
     */
    public void selectPreviousTab(DataPackage context)
    {
        Project proj = context.getProject();
        FileEditorManager fem = FileEditorManager.getInstance(proj); // API change - don't merge
        VirtualFile vf = (VirtualFile)lastSelections.get(fem);
        if (vf != null)
        {
            fem.openFile(vf, true);
        }
        else
        {
            VimPlugin.indicateError();
        }
    }

    public Editor selectEditor(Project project, VirtualFile file)
    {
        FileEditorManager fMgr = FileEditorManager.getInstance(project);
        FileEditor[] feditors = fMgr.openFile(file, true);
        if (feditors != null && feditors.length > 0)
        {
            if (feditors[0] instanceof TextEditor)
            {
                return ((TextEditor)feditors[0]).getEditor();
            }
        }

        return null;
    }

    public void displayAsciiInfo(Editor editor)
    {
        int offset = editor.getCaretModel().getOffset();
        char ch = editor.getDocument().getCharsSequence().charAt(offset);

        StringBuffer msg = new StringBuffer();
        msg.append('<');
        msg.append(StringHelper.escape("" + ch));
        msg.append(">  ");
        msg.append((int)ch);
        msg.append(",  Hex ");
        msg.append(Long.toHexString(ch));
        msg.append(",  Octal ");
        msg.append(Long.toOctalString(ch));

        VimPlugin.showMessage(msg.toString());
    }

    public void displayHexInfo(Editor editor)
    {
        int offset = editor.getCaretModel().getOffset();
        char ch = editor.getDocument().getCharsSequence().charAt(offset);

        VimPlugin.showMessage(Long.toHexString(ch));
    }

    public void displayLocationInfo(Editor editor)
    {
        StringBuffer msg = new StringBuffer();
        Document doc = editor.getDocument();

        if (CommandState.getInstance(editor).getMode() != CommandState.MODE_VISUAL)
        {
            LogicalPosition lp = editor.getCaretModel().getLogicalPosition();
            int col = editor.getCaretModel().getOffset() - doc.getLineStartOffset(lp.line);
            int endoff = doc.getLineEndOffset(lp.line);
            if (doc.getCharsSequence().charAt(endoff) == '\n')
            {
                endoff--;
            }
            int ecol = endoff - doc.getLineStartOffset(lp.line);
            LogicalPosition elp = editor.offsetToLogicalPosition(endoff);

            msg.append("Col ").append(col + 1);
            if (col != lp.column)
            {
                msg.append("-").append(lp.column + 1);
            }

            msg.append(" of ").append(ecol + 1);
            if (ecol != elp.column)
            {
                msg.append("-").append(elp.column + 1);
            }

            int lline = EditorHelper.getCurrentLogicalLine(editor);
            int total = EditorHelper.getLineCount(editor);

            msg.append("; Line ").append(lline + 1).append(" of " ).append(total);

            SearchHelper.CountPosition cp = SearchHelper.countWords(editor);

            msg.append("; Word ").append(cp.getPosition()).append(" of ").append(cp.getCount());

            int offset = editor.getCaretModel().getOffset();
            int size = EditorHelper.getFileSize(editor);

            msg.append("; Character ").append(offset + 1).append(" of " ).append(size);
        }
        else
        {
            msg.append("Selected ");

            TextRange vr = CommandGroups.getInstance().getMotion().getVisualRange(editor);
            vr.normalize();

            int lines;
            SearchHelper.CountPosition cp = SearchHelper.countWords(editor);
            int words = cp.getCount();
            int word = 0;
            if (vr.isMultiple())
            {
                lines = vr.size();
                int cols = vr.getMaxLength();

                msg.append(cols).append(" Cols; ");

                for (int i = 0; i < vr.size(); i++)
                {
                    cp = SearchHelper.countWords(editor, vr.getStartOffsets()[i], vr.getEndOffsets()[i] - 1);
                    word += cp.getCount();
                }
            }
            else
            {
                LogicalPosition slp = editor.offsetToLogicalPosition(vr.getStartOffset());
                LogicalPosition elp = editor.offsetToLogicalPosition(vr.getEndOffset());

                lines = elp.line - slp.line + 1;

                cp = SearchHelper.countWords(editor, vr.getStartOffset(), vr.getEndOffset() - 1);
                word = cp.getCount();
            }

            int total = EditorHelper.getLineCount(editor);

            msg.append(lines).append(" of ").append(total).append(" Lines");

            msg.append("; ").append(word).append(" of ").append(words).append(" Words");

            int chars = vr.getSelectionCount();
            int size = EditorHelper.getFileSize(editor);

            msg.append("; ").append(chars).append(" of ").append(size).append(" Characters");
        }

        VimPlugin.showMessage(msg.toString());
    }

    public void displayFileInfo(Editor editor, boolean fullPath)
    {
        StringBuffer msg = new StringBuffer();
        VirtualFile vf = EditorData.getVirtualFile(editor);
        if (vf != null)
        {
            msg.append('"');
            if (fullPath)
            {
                msg.append(vf.getPath());
            }
            else
            {
                Project project = EditorData.getProject(editor);
                VirtualFile root = ProjectRootManager.getInstance(project).getFileIndex().getContentRootForFile(vf);
                if (root != null)
                {
                    msg.append(vf.getPath().substring(root.getPath().length() + 1));
                }
                else
                {
                    msg.append(vf.getPath());
                }
            }
            msg.append("\" ");
        }
        else
        {
            msg.append("\"[No File]\" ");
        }

        Document doc = editor.getDocument();
        if (!doc.isWritable())
        {
            msg.append("[RO] ");
        }
        else if (FileDocumentManager.getInstance().isDocumentUnsaved(doc))
        {
            msg.append("[+] ");
        }

        int lline = EditorHelper.getCurrentLogicalLine(editor);
        int total = EditorHelper.getLineCount(editor);
        int pct = (int)((float)lline / (float)total * 100f + 0.5);

        msg.append("line ").append(lline + 1).append(" of " ).append(total);
        msg.append(" --").append(pct).append("%-- ");

        LogicalPosition lp = editor.getCaretModel().getLogicalPosition();
        int col = editor.getCaretModel().getOffset() - doc.getLineStartOffset(lline);

        msg.append("col ").append(col + 1);
        if (col != lp.column)
        {
            msg.append("-").append(lp.column + 1);
        }

        VimPlugin.showMessage(msg.toString());
    }

    /**
     * This class listens for editor tab changes so any insert/replace modes that need to be reset can be
     */
    public static class SelectionCheck extends FileEditorManagerAdapter
    {
        /**
         * The user has changed the editor they are working with - exit insert/replace mode, and complete any
         * appropriate repeat.
         *
         * @param event
         */
        public void selectionChanged(FileEditorManagerEvent event)
        {
            lastSelections.put(event.getManager(), event.getOldFile());
        }
    }

    private static HashMap lastSelections = new HashMap();

    private static Logger logger = Logger.getInstance(FileGroup.class.getName());
}
