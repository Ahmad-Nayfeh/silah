package io.github.ahmadnayfeh.silah.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.ahmadnayfeh.silah.domain.Channel
import io.github.ahmadnayfeh.silah.domain.Direction
import io.github.ahmadnayfeh.silah.domain.Source
import io.github.ahmadnayfeh.silah.domain.Tag
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "people")
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val tag: Tag,
    /** International form, e.g. +966501234567. */
    val phone: String? = null,
    val targetDays: Int,
    val note: String = "",
    val isPaused: Boolean = false,
    /** Epoch millis. */
    val createdAt: Long,
    /** Epoch day of the last "not today", if any. */
    val skippedOn: Long? = null,
)

@Serializable
@Entity(
    tableName = "contacts",
    foreignKeys = [ForeignKey(Person::class, ["id"], ["personId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("personId")],
)
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    /** Epoch millis. */
    val dateTime: Long,
    val direction: Direction,
    val channel: Channel,
    val source: Source,
)

@Serializable
@Entity(
    tableName = "threads",
    foreignKeys = [ForeignKey(Person::class, ["id"], ["personId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("personId")],
)
data class Thread(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val text: String,
    val createdAt: Long,
    val isDone: Boolean = false,
)

@Serializable
@Entity(
    tableName = "occasions",
    foreignKeys = [ForeignKey(Person::class, ["id"], ["personId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("personId")],
)
data class Occasion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val title: String,
    /** Epoch day. */
    val date: Long,
    val repeatsYearly: Boolean,
    val remindDaysBefore: Int = 2,
)
