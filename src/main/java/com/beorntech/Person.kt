package com.beorntech

import java.util.*

/**
 * Beorn Java Bean Person
 */
class Person {
    var name: String
    var isMarried: Boolean
    var spouse: String
    var children: List<String>

    constructor() {
        name = ""
        isMarried = false
        spouse = ""
        children = emptyList()
    }

    constructor(name: String, spouse: String, isMarried: Boolean, numberOfChildren: Int) : super() {
        this.name = name
        this.spouse = spouse
        this.isMarried = isMarried
        children = ArrayList()
        for (i in 0 until numberOfChildren) {
            (children as ArrayList<String>).add("Child $i")
        }
    }

    override fun toString(): String {
        return ("Person [name=" + name + ", married=" + isMarried + ", spouse="
                + spouse + ", children=" + children + "]")
    }

    companion object {
        private val persons: ArrayList<Person> = arrayListOf(
                Person("Kerstin", "Jose", false, 1),
                Person("Erik", "Dora", true, 3),
                Person("Svajune", "Thomas", true, 0)
        )

        @JvmStatic
        fun lookup(id: String?): Person {
            if (id != null) {
                val personId = id.toInt()
                if (personId > 0 && personId <= persons.size) {
                    return persons[personId - 1]!!
                }
            }
            return Person("Empty Name", "Empty spouse", false, 0)
        }
    }
}