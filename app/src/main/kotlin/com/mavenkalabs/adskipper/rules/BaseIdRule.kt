package com.mavenkalabs.adskipper.rules

abstract class BaseIdRule(id: String, packageName: String) : BaseRule {
    protected val qualifiedId: String = "$packageName:id/$id"

    val id
        get() = qualifiedId.split("/".toRegex()).last()
}
