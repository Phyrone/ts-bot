package de.phyrone.tsbot.util

import com.github.manevolent.ts3j.api.Client
import de.phyrone.tsbot.config.Config
import de.phyrone.tsbot.config.PermissionGroupConfig

fun isAllowed(permission: String, allowed: List<String>) = allowed
    .map { allowedPermission -> permissionToPattern(allowedPermission) }
    .any { regex -> regex.containsMatchIn(permission) }


fun permissionToPattern(permission: String) = Regex(
    permission.toLowerCase()
        .replace(Regex("[^A-Za-z0-9.\\-*]+"), "")
        .replace("*", ".+")
)

fun groupsToPermissionsList(groups: List<PermissionGroupConfig>): List<String> = mutableListOf<String>().also { list ->
    groups.map { group -> group.permissions }.forEach { permissions -> list.addAll(permissions) }
}

fun filterGroups(client: Client, groups: List<PermissionGroupConfig>) =
    groups.filter { group ->
        group.users.contains(client.uniqueIdentifier) ||
                group.groups.any { serverGroup ->
                    client.isInServerGroup(serverGroup)
                }
    }

fun hasPermission(permission: String, client: Client, groups: List<PermissionGroupConfig> = Config.config.permissions) =
    isAllowed(permission, groupsToPermissionsList(filterGroups(client, groups)))

fun main(args: Array<String>) {
    val allowed = isAllowed(
        "bot.shutdown",
        listOf(
            "bo"
        )
    )
    println(allowed)
}