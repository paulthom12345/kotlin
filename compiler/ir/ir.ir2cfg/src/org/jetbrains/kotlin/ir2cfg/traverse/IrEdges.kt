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

class IrEdges(val previous: List<IrElement>, val next: List<IrElement>) {
    constructor(previous: IrElement, next: IrElement): this(listOf(previous), listOf(next))

    constructor(previous: IrElement, next: List<IrElement>): this(listOf(previous), next)

    constructor(previous: List<IrElement>, next: IrElement): this(previous, listOf(next))
}

val noEdges = IrEdges(listOf(), listOf())

fun startEdges(vararg next: IrElement) = IrEdges(listOf(), next.toList())

fun endEdges(vararg previous: IrElement) = IrEdges(previous.toList(), listOf())

