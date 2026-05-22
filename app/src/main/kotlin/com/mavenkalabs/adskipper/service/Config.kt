package com.mavenkalabs.adskipper.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import com.mavenkalabs.adskipper.actions.Action
import com.mavenkalabs.adskipper.rules.BaseRule
import com.mavenkalabs.adskipper.rules.RuleResult
import com.mavenkalabs.adskipper.rules.RulesParser
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicReference


const val CONFIG_JSON_FILE = "config.json"

fun loadConfig(context: Context) : Map<String, List<Handler>> {
    val packages = Json.decodeFromString<List<Package>>(
        context.assets.open(CONFIG_JSON_FILE).bufferedReader().readText()
    )

    val rulesParser = RulesParser()
    packages.forEach { p -> p.handlers.forEach { h -> if (h.selector.conditions != null) {
            h.selector.rule = rulesParser.orRules(*h.selector.conditions
                .map { s -> rulesParser.parse(s, p.packageName) }
                .toTypedArray())
        }
    } }
    return packages.associate { p -> p.packageName to p.handlers }
}

@Serializable
data class Package(val packageName: String, val handlers: List<Handler>)

@Serializable
data class Handler (val selector: Selector, val selectedAction: Action? = null, val unselectedAction: Action? = null) {
    fun apply(service: AccessibilityService, event: AccessibilityEvent) : Boolean {
        val extras = extrasRef.get()
        val eventSelectionResult = selector.apply(service, event, extras)
        val result = if (eventSelectionResult.isPassed) {
            selectedAction?.apply(service, extras,eventSelectionResult.filteredNodes?.first())?: false
        } else {
            unselectedAction?.apply(service, extras)?: false
        }
        extrasRef.set(extras)
        return result
    }

    companion object{
        internal val extrasRef = AtomicReference(mutableMapOf<String, Any>())
    }
}

@Serializable
data class Selector(val eventTypes: List<Int>? = null, val conditions: List<String>? = null) {
    var rule : BaseRule? = null

    fun apply(
        service: AccessibilityService,
        event: AccessibilityEvent,
        parameters: MutableMap<String, Any>? = null
    ) : RuleResult {
        val isPassed = eventTypes?.let {
            event.eventType in eventTypes
        }?:true

        if (!isPassed) {
            return RuleResult(false)
        } else {
            if (rule != null) {
                val rootNode = service.rootInActiveWindow
                return if (rootNode != null) {
                    rule!!.apply(rootNode, parameters)
                } else {
                    RuleResult(isPassed = false)
                }
            } else {
                return RuleResult(true)
            }
        }
    }
}