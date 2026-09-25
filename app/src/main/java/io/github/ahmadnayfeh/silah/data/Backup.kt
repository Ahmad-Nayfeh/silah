package io.github.ahmadnayfeh.silah.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The full backup file: every table plus the user's settings. */
@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val version: Int = 1,
    val exportedAt: Long,
    val settings: Settings,
    val people: List<Person>,
    val contacts: List<Contact>,
    val threads: List<Thread>,
    val occasions: List<Occasion>,
) {
    companion object {
        const val FORMAT = "silah-backup"
    }
}

class BackupManager(private val repo: SilahRepository) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun export(): String {
        val s = repo.currentSnapshot()
        val file = BackupFile(
            exportedAt = repo.now(),
            settings = repo.store.currentSettings(),
            people = s.people,
            contacts = s.contacts,
            threads = s.threads,
            occasions = s.occasions,
        )
        return json.encodeToString(BackupFile.serializer(), file)
    }

    /** Parses first, so a broken file never touches the existing data. */
    fun parse(text: String): BackupFile {
        val file = json.decodeFromString(BackupFile.serializer(), text)
        require(file.format == BackupFile.FORMAT) { "not a silah backup" }
        return file
    }

    /** Replaces everything with the backup's contents. */
    suspend fun restore(file: BackupFile) {
        repo.replaceAll(file.people, file.contacts, file.threads, file.occasions)
        repo.store.saveSettings(file.settings)
        repo.store.updateState { it.copy(pickDay = null, pickPerson = null, onboarded = true) }
    }
}
