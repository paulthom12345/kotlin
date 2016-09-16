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

// Does we really need this?
class IrStatementTraverser(statement: IrStatement) : AbstractIrTraverser() {
    override val previousElements: List<IrElement>
        get() = listOf()
    override val nextElements: List<IrElement>
        get() = listOf()

    override fun onNext(next: IrElement) {
        TODO("not implemented")
    }

    override fun onPrevious(previous: IrElement) {
        TODO("not implemented")
    }
}