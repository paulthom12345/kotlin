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
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class IrWhenTraverser(val irWhen: IrWhen, atStart: Boolean) : AbstractIrTraverser() {

    inner class IrWhenSink : IrElement {
        override val startOffset = irWhen.startOffset
        override val endOffset = irWhen.endOffset

        override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D) = visitor.visitElement(this, data)

        override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        }

        override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        }
    }

    val whenSink = IrWhenSink()

    private var index = if (atStart) -1 else if (irWhen.elseBranch == null) irWhen.branchesCount - 1 else irWhen.branchesCount

    private enum class State {
        CONDITION,
        RESULT
    }

    private var state = State.RESULT

    private val stateExpression: IrExpression? get() = when (state) {
        State.CONDITION -> irWhen.getNthCondition(index)
        State.RESULT -> when (index) {
            irWhen.branchesCount -> irWhen.elseBranch
            else -> irWhen.getNthResult(index)
        }
    }

    private var expressionTraverser = stateExpression?.traverser(atStart)

    private fun conditionOrNull(i: Int) =
            if (i in 0..irWhen.branchesCount - 1) irWhen.getNthCondition(i) else null

    private fun resultOrNull(i: Int) =
            if (i in 0..irWhen.branchesCount - 1) irWhen.getNthResult(i) else if (i == irWhen.branchesCount) irWhen.elseBranch else null

    private val hasNestedNext: Boolean get() = expressionTraverser.hasNext()

    override val nextElements: List<IrElement>
        get() {
            return if (hasNestedNext) expressionTraverser!!.nextElements
            else when (state) {
                State.RESULT -> {
                    if (index == -1) listOfNotNull(conditionOrNull(index + 1))
                    else listOfNotNull(whenSink)
                }
                State.CONDITION -> {
                    if (index < irWhen.branchesCount - 1) listOfNotNull(resultOrNull(index), conditionOrNull(index + 1))
                    else listOfNotNull(resultOrNull(index), resultOrNull(index + 1))
                }
            }
        }

    override fun onNext(next: IrElement) {
        if (hasNestedNext) {
            expressionTraverser!!.next(next)
        }
        else {
            when (state) {
                State.CONDITION -> {
                    val nextResult = resultOrNull(index)
                    if (next === nextResult) {
                        state = State.RESULT
                    }
                    else {
                        index++
                        if (index >= irWhen.branchesCount) {
                            state = State.RESULT
                        }
                    }
                }
                State.RESULT -> {
                    index++
                    if (index > irWhen.branchesCount + 2) {
                        throw IllegalStateException("When traverser is already at end")
                    }
                    else if (index == 0) {
                        state = State.CONDITION
                    }
                    else if (index < irWhen.branchesCount) {
                        index = irWhen.branchesCount + 1
                    }
                }
            }
            expressionTraverser = stateExpression?.traverserAtStart()
        }
    }

    private val hasNestedPrevious: Boolean get() = expressionTraverser.hasPrevious()

    override val previousElements: List<IrElement>
        get() {
            return if (hasNestedPrevious) expressionTraverser!!.previousElements
            else when (state) {
                State.RESULT -> when (index) {
                    irWhen.branchesCount + 2 -> listOfNotNull(whenSink)
                    irWhen.branchesCount + 1 -> listOfNotNull(resultOrNull(index - 1))
                    irWhen.branchesCount -> listOfNotNull(conditionOrNull(index - 1))
                    else -> listOfNotNull(conditionOrNull(index))
                }
                State.CONDITION -> listOfNotNull(conditionOrNull(index - 1))
            }
        }

    override fun onPrevious(previous: IrElement) {
        if (hasNestedPrevious) {
            expressionTraverser!!.previous(previous)
        }
        else {
            when (state) {
                State.RESULT -> {
                    if (index == -1) {
                        throw IllegalStateException("When traverser is at start")
                    }
                    else if (index < irWhen.branchesCount) {
                        state = State.CONDITION
                    }
                    else {
                        index--
                    }
                }
                State.CONDITION -> {
                    index--
                    if (index < 0) {
                        state = State.RESULT
                    }
                }
            }
            expressionTraverser = stateExpression?.traverserAtEnd()
        }
    }
}