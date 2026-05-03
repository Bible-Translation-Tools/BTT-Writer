package org.bibletranslationtools.writer.core

interface ResourceProvider {
    fun getString(resourceId: Int): String
    fun getStringArray(resourceId: Int): List<String>
}