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

package org.jetbrains.kotlin.ir2cfg.traverse

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer

class IrContainerTraverser(private val container: IrStatementContainer, atStart: Boolean) : AbstractIrTraverser() {

    private val statements: List<IrStatement> get() = container.statements

    private var position = if (atStart) -1 else container.statements.size - 1

    private val statement: IrStatement? = statements.getOrNull(position)

    private var statementTraverser: IrTraverser? = statement?.traverser(atStart)

    override val nextElements: List<IrElement>
        get() = if (hasNestedNext) statementTraverser!!.nextElements else listOfNotNull(statements.getOrNull(position + 1))

    private val hasNestedNext: Boolean get() = statementTraverser.hasNext()

    override fun onNext(next: IrElement) {
        if (hasNestedNext) {
            statementTraverser!!.next(next)
        }
        else {
            position++
            statementTraverser = statement?.traverserAtStart()
        }
    }

    override val previousElements: List<IrElement>
        get() = if (hasNestedPrevious) statementTraverser!!.previousElements else listOfNotNull(statement)

    private val hasNestedPrevious: Boolean get() = statementTraverser.hasPrevious()

    override fun onPrevious(previous: IrElement) {
        if (hasNestedPrevious) {
            statementTraverser!!.previous(previous)
        }
        else {
            position--
            statementTraverser = statement?.traverserAtEnd()
        }
    }
}