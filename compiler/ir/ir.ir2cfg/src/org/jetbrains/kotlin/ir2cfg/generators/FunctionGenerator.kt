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

package org.jetbrains.kotlin.ir2cfg.generators

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir2cfg.graph.ControlFlowGraph
import org.jetbrains.kotlin.ir2cfg.traverse.IrFunctionTraverser
import java.util.*

class FunctionGenerator(val function: IrFunction) {

    fun generate(): ControlFlowGraph {
        val builder = FunctionBuilder(function)
        val traverser = IrFunctionTraverser(function)
        val queue: Queue<IrElement> = LinkedList<IrElement>()
        while (traverser.hasNext()) {
            val current = traverser.next()
            val nextElements = traverser.nextElements
            val previousElements = traverser.previousElements
            if (nextElements.size < 2 && previousElements.size < 2) {
                builder.add(current)
            }
            else {
                builder.jump(current)
            }
        }
        return builder.build()
    }
}