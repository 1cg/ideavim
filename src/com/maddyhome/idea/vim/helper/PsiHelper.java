package com.maddyhome.idea.vim.helper;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.lang.LanguageStructureViewBuilder;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.TIntArrayList;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2006 Rick Maddy
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

public class PsiHelper
{
    public static int findMethodStart(Editor editor, int offset, int count)
    {
        return findMethodOrClass(editor, offset, count, true);
    }

    public static int findMethodEnd(Editor editor, int offset, int count)
    {
        return findMethodOrClass(editor, offset, count, false);
    }

    private static int findMethodOrClass(Editor editor, int offset, int count, boolean isStart)
    {
        PsiFile file = getFile(editor);

        StructureViewBuilder structureViewBuilder = LanguageStructureViewBuilder.INSTANCE.getStructureViewBuilder(file);
        if (!(structureViewBuilder instanceof TreeBasedStructureViewBuilder)) return -1;
        TreeBasedStructureViewBuilder builder = (TreeBasedStructureViewBuilder) structureViewBuilder;
        StructureViewModel model = builder.createStructureViewModel();

        TIntArrayList navigationOffsets = new TIntArrayList();
        addNavigationElements(model.getRoot(), navigationOffsets, isStart);
        navigationOffsets.sort();

        int index = navigationOffsets.size();
        for (int i = 0; i < navigationOffsets.size(); i++)
        {
            if (navigationOffsets.get(i) > offset)
            {
                index = i;
                if (count > 0) count--;
                break;
            }
            else if (navigationOffsets.get(i) == offset)
            {
                index = i;
                break;
            }
        }
        int resultIndex = index + count;
        if (resultIndex < 0)
        {
            resultIndex = 0;
        }
        else if (resultIndex >= navigationOffsets.size())
        {
            resultIndex = navigationOffsets.size()-1;
        }

        return navigationOffsets.get(resultIndex);
    }

    private static void addNavigationElements(TreeElement root, TIntArrayList navigationOffsets, boolean start)
    {
        if (root instanceof PsiTreeElementBase)
        {
            PsiElement element = ((PsiTreeElementBase) root).getValue();
            int offset;
            if (start)
            {
                offset = element.getTextRange().getStartOffset();
                if (element.getLanguage().getID().equals("JAVA"))
                {
                    // HACK: for Java classes and methods, we want to jump to the opening brace
                    int textOffset = element.getTextOffset();
                    int braceIndex = element.getText().indexOf('{', textOffset - offset);
                    if (braceIndex >= 0)
                    {
                        offset += braceIndex;
                    }
                }
            }
            else
            {
                offset = element.getTextRange().getEndOffset()-1;
            }
            if (!navigationOffsets.contains(offset))
            {
                navigationOffsets.add(offset);
            }
        }
        for (TreeElement child : root.getChildren())
        {
            addNavigationElements(child, navigationOffsets, start);
        }
    }

    private static PsiFile getFile(Editor editor)
    {
        VirtualFile vf = EditorData.getVirtualFile(editor);
        Project proj = EditorData.getProject(editor);
        PsiManager mgr = PsiManager.getInstance(proj);

        return mgr.findFile(vf);
    }
}
