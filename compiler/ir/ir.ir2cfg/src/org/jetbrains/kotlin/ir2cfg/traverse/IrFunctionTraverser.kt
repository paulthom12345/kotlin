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
import org.jetbrains.kotlin.ir.declarations.IrFunction

class IrFunctionTraverser(val function: IrFunction) : AbstractIrTraverser() {

    private var bodyTraverser: IrTraverser? = null

    override val previousElements: List<IrElement>
        get() = bodyTraverser?.previousElements?.let { if (it.isEmpty()) listOf(function) else it } ?: listOf()
    override val nextElements: List<IrElement>
        get() = bodyTraverser?.nextElements ?: listOf(function)

    override fun onNext(next: IrElement) {
        val traverser = bodyTraverser
                        ?: function.body?.traverser()
                        ?: throw IllegalStateException("Next on function traverser without body")
        if (next !== function) {
            traverser.next(next)
        }
    }

    override fun onPrevious(previous: IrElement) {
        val traverser = bodyTraverser ?: throw IllegalStateException("Function traverser already at start")
        if (traverser.hasPrevious() && previous !== function) {
            traverser.previous(previous)
        }
        else {
            bodyTraverser = null
        }
    }
}