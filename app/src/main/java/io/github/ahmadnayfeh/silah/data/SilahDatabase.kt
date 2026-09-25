package io.github.ahmadnayfeh.silah.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import io.github.ahmadnayfeh.silah.domain.Channel
import io.github.ahmadnayfeh.silah.domain.Direction
import io.github.ahmadnayfeh.silah.domain.Source
import io.github.ahmadnayfeh.silah.domain.Tag
import kotlinx.coroutines.flow.Flow

class Converters {
    @TypeConverter fun tagToString(v: Tag) = v.name
    @TypeConverter fun stringToTag(v: String) = Tag.valueOf(v)
    @TypeConverter fun directionToString(v: Direction) = v.name
    @TypeConverter fun stringToDirection(v: String) = Direction.valueOf(v)
    @TypeConverter fun channelToString(v: Channel) = v.name
    @TypeConverter fun stringToChannel(v: String) = Channel.valueOf(v)
    @TypeConverter fun sourceToString(v: Source) = v.name
    @TypeConverter fun stringToSource(v: String) = Source.valueOf(v)
}

@Dao
interface SilahDao {
    @Query("SELECT * FROM people ORDER BY name") fun people(): Flow<List<Person>>
    @Query("SELECT * FROM contacts ORDER BY dateTime DESC") fun contacts(): Flow<List<Contact>>
    @Query("SELECT * FROM threads ORDER BY createdAt DESC") fun threads(): Flow<List<Thread>>
    @Query("SELECT * FROM occasions ORDER BY date") fun occasions(): Flow<List<Occasion>>

    @Query("SELECT * FROM people") suspend fun allPeople(): List<Person>
    @Query("SELECT * FROM contacts") suspend fun allContacts(): List<Contact>
    @Query("SELECT * FROM threads") suspend fun allThreads(): List<Thread>
    @Query("SELECT * FROM occasions") suspend fun allOccasions(): List<Occasion>

    @Query("SELECT * FROM people WHERE id = :id") suspend fun person(id: Long): Person?

    @Insert suspend fun insert(person: Person): Long
    @Update suspend fun update(person: Person)
    @Delete suspend fun delete(person: Person)

    @Insert suspend fun insert(contact: Contact): Long
    @Query("DELETE FROM contacts WHERE id = :id") suspend fun deleteContact(id: Long)

    @Insert suspend fun insert(thread: Thread): Long
    @Update suspend fun update(thread: Thread)
    @Query("DELETE FROM threads WHERE id = :id") suspend fun deleteThread(id: Long)

    @Insert suspend fun insert(occasion: Occasion): Long
    @Query("DELETE FROM occasions WHERE id = :id") suspend fun deleteOccasion(id: Long)

    @Query("UPDATE people SET skippedOn = :epochDay WHERE id = :id") suspend fun setSkipped(id: Long, epochDay: Long?)

    // Bulk operations used by backup restore.
    @Insert suspend fun insertPeople(items: List<Person>)
    @Insert suspend fun insertContacts(items: List<Contact>)
    @Insert suspend fun insertThreads(items: List<Thread>)
    @Insert suspend fun insertOccasions(items: List<Occasion>)
    @Query("DELETE FROM people") suspend fun clearPeople()
    @Query("DELETE FROM contacts") suspend fun clearContacts()
    @Query("DELETE FROM threads") suspend fun clearThreads()
    @Query("DELETE FROM occasions") suspend fun clearOccasions()
}

@Database(
    entities = [Person::class, Contact::class, Thread::class, Occasion::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SilahDatabase : RoomDatabase() {
    abstract fun dao(): SilahDao

    companion object {
        fun create(context: Context): SilahDatabase =
            Room.databaseBuilder(context, SilahDatabase::class.java, "silah.db").build()

        fun inMemory(context: Context): SilahDatabase =
            Room.inMemoryDatabaseBuilder(context, SilahDatabase::class.java).allowMainThreadQueries().build()
    }
}
