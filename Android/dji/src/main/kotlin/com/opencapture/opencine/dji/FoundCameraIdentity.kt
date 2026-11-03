package com.opencapture.opencine.dji

/** Scan-row identity. Lockstep with Swift `FoundCameraIdentity`. */
object FoundCameraIdentity {
    fun isGenericName(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return true
        val compact = trimmed.lowercase()
        return compact == "dji camera" || compact == "dji osmo camera" || compact == "dji osmo 相机"
    }

    fun listTitle(advertisedName: String, modelName: String): String =
        if (isGenericName(advertisedName)) modelName else advertisedName

    fun listSubtitle(advertisedName: String, modelName: String, family: String): String {
        val kind =
            when (family.lowercase()) {
                "nano" -> "Nano"
                "other" -> modelName
                else -> "Pocket"
            }
        return if (isGenericName(advertisedName) || advertisedName == modelName) {
            "$kind · 附近"
        } else {
            "$kind · $modelName · 附近"
        }
    }

    fun shouldReplace(
        existingName: String,
        existingModelId: Int?,
        incomingName: String,
        incomingModelId: Int?,
    ): Boolean {
        if (isGenericName(incomingName)) {
            return isGenericName(existingName) && existingModelId == null && incomingModelId != null
        }
        if (isGenericName(existingName)) return true
        if (existingName.trim() != incomingName.trim()) return true
        return existingModelId == null && incomingModelId != null
    }
}
