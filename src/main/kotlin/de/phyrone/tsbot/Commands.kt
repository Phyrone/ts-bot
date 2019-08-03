package de.phyrone.tsbot

data class ExecutedCommand(val command: String, val sender: CommandSender)
interface CommandSender {
    fun hasPermission(permission: String): Boolean
    fun sendMessage(message: String)
}

fun CommandSender.sendMessages(messages: Iterable<String>) {
    messages.forEach { message ->
        this.sendMessage(message)
    }
}

fun CommandSender.sendMessages(vararg messages: String) {
    messages.forEach { message ->
        this.sendMessage(message)
    }
}