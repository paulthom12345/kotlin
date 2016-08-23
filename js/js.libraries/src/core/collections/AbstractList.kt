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
/*
 * Based on GWT AbstractList
 * Copyright 2007 Google Inc.
*/


package kotlin.collections

public abstract class AbstractList<E> protected constructor() : AbstractCollection<E>(), MutableList<E> {
    abstract override val size: Int
    abstract override fun get(index: Int): E

    protected var modCount: Int = 0

    override fun add(index: Int, element: E): Unit = throw UnsupportedOperationException("Add not supported on this list")
    override fun removeAt(index: Int): E = throw UnsupportedOperationException("Remove not supported on this list")
    override fun set(index: Int, element: E): E = throw UnsupportedOperationException("Set not supported on this list")

    override fun add(element: E): Boolean {
        add(size, element)
        return true
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        var _index = index
        var changed = false
        for (e in elements) {
            add(_index++, e)
            changed = true
        }
        return changed
    }

    override fun clear() {
        removeRange(0, size)
    }


    override fun iterator(): MutableIterator<E> = IteratorImpl()

    override fun indexOf(element: E): Int {
        for (index in 0..lastIndex) {
            if (get(index) == element) {
                return index
            }
        }
        return -1
    }

    override fun lastIndexOf(element: E): Int {
        for (index in lastIndex downTo 0) {
            if (get(index) == element) {
                return index
            }
        }
        return -1
    }

    override fun listIterator(): MutableListIterator<E> = listIterator(0)
    override fun listIterator(index: Int): MutableListIterator<E> = ListIteratorImpl(index)


    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> = SubList(this, fromIndex, toIndex)

    protected open fun removeRange(fromIndex: Int, toIndex: Int) {
        val iterator = listIterator(fromIndex)
        repeat(toIndex - fromIndex) {
            iterator.next()
            iterator.remove()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is List<*>) return false
        if (size != other.size) return false

        val otherIterator = other.iterator()
        for (elem in this) {
            val elemOther = otherIterator.next()
            if (elem != elemOther) {
                return false
            }
        }

        return true
    }

    override fun hashCode(): Int {
        var hashCode = 1
        for (e in this) {
            hashCode = 31 * hashCode + (e?.hashCode() ?: 0)
            hashCode = hashCode or 0 // make sure we don't overflow
        }
        return hashCode
    }


    private open inner class IteratorImpl : MutableIterator<E> {
        /** the index of the item that will be returned on the next call to [next]`()` */
        protected var index = 0
        /** the index of the item that was returned on the previous call to [next]`()`
         * or [ListIterator.previous]`()` (for `ListIterator`),
         * -1 if no such item exists
         */
        protected var last = -1

        override fun hasNext(): Boolean = index < size

        override fun next(): E {
            if (!hasNext()) throw NoSuchElementException()
            last = index++
            return get(last)
        }

        override fun remove() {
            check(last != -1)

            removeAt(last)
            index = last
            last = -1
        }
    }

    /**
     * Implementation of `MutableListIterator` for abstract lists.
     */
    private inner class ListIteratorImpl(index: Int) : IteratorImpl(), MutableListIterator<E> {

        init {
            checkPositionIndex(index, this@AbstractList.size)
            this.index = index
        }

        override fun hasPrevious(): Boolean = index > 0

        override fun nextIndex(): Int = index

        override fun previous(): E {
            if (!hasPrevious()) throw NoSuchElementException()

            last = --index
            return get(last)
        }

        override fun previousIndex(): Int = index - 1

        override fun add(element: E) {
            add(index, element)
            index++
            last = -1
        }

        override fun set(element: E) {
            require(last != -1)
            this@AbstractList[last] = element
        }
    }

    private class SubList<E>(private val list: AbstractList<E>, private val fromIndex: Int, toIndex: Int) : AbstractList<E>() {
        private var _size: Int = 0

        init {
            checkRangeIndexes(fromIndex, toIndex, list.size)
            this._size = toIndex - fromIndex
        }

        override fun add(index: Int, element: E) {
            checkPositionIndex(index, _size)

            list.add(fromIndex + index, element)
            _size++
        }

        override fun get(index: Int): E {
            checkElementIndex(index, _size)

            return list[fromIndex + index]
        }

        override fun removeAt(index: Int): E {
            checkElementIndex(index, _size)

            val result = list.removeAt(fromIndex + index)
            _size--
            return result
        }

        override fun set(index: Int, element: E): E {
            checkElementIndex(index, _size)

            return list.set(fromIndex + index, element)
        }

        override val size: Int get() = _size
    }

    companion object {
        internal fun checkElementIndex(index: Int, size: Int) {
            if (index < 0 || index >= size) {
                throw IndexOutOfBoundsException("index: $index, size: $size")
            }
        }

        internal fun checkPositionIndex(index: Int, size: Int) {
            if (index < 0 || index > size) {
                throw IndexOutOfBoundsException("index: $index, size: $size")
            }
        }

        internal fun checkRangeIndexes(start: Int, end: Int, size: Int) {
            if (start < 0 || end > size) {
                throw IndexOutOfBoundsException("fromIndex: $start, toIndex: $end, size: $size")
            }
            if (start > end) {
                throw IllegalArgumentException("fromIndex: $start > toIndex: $end")
            }
        }
    }

}
