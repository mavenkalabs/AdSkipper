package com.mavenkalabs.adskipper.rules

abstract class BaseIdRule(indexedId: String, packageName: String) : BaseRule {
    private val idRegex = """^(.+?)(?:\[(\d+)])?$""".toRegex()

    val id = idRegex.find(indexedId)?.groupValues[1]?:indexedId
    val index = idRegex.find(indexedId)?.groupValues[2]?.toIntOrNull()

    protected val qualifiedId: String = "$packageName:id/$id"
}
