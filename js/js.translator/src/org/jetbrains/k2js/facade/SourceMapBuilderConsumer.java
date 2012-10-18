/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.k2js.facade;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.PairConsumer;
import org.jetbrains.js.compiler.SourceMapBuilder;

class SourceMapBuilderConsumer implements PairConsumer<SourceMapBuilder, Object> {
    //private PsiElement prev;

    @Override
    public void consume(SourceMapBuilder builder, Object sourceInfo) {
        if (!(sourceInfo instanceof PsiElement)) {
            return;
        }

        PsiElement element = (PsiElement) sourceInfo;
        PsiFile file = element.getContainingFile();
        int offset = element.getNode().getStartOffset();
        Document document = file.getViewProvider().getDocument();
        assert document != null;
        int line = document.getLineNumber(offset);
        int column = offset - document.getLineStartOffset(line);
        builder.addMapping(file.getViewProvider().getVirtualFile().getPath(), line, column);

        //prev = element;
    }
}
