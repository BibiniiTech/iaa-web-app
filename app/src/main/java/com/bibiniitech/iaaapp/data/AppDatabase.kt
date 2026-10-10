package com.bibiniitech.iaaapp.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "submission_drafts")
data class SubmissionDraft(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // quarterly, annual, ccc, soi, other
    val institutionType: String,
    val institution: String,
    val region: String,
    val period: String,
    val senderName: String,
    val senderEmail: String,
    val lastUpdated: Long = System.currentTimeMillis(),
)

@Dao
interface SubmissionDraftDao {
    @Query("SELECT * FROM submission_drafts ORDER BY lastUpdated DESC")
    fun getAllDrafts(): Flow<List<SubmissionDraft>>

    @Query("SELECT * FROM submission_drafts WHERE category = :category LIMIT 1")
    suspend fun getDraftByCategory(category: String): SubmissionDraft?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: SubmissionDraft)

    @Query("DELETE FROM submission_drafts WHERE id = :id")
    suspend fun deleteDraft(id: Int)
    
    @Query("DELETE FROM submission_drafts WHERE category = :category")
    suspend fun deleteDraftByCategory(category: String)

    @Transaction
    suspend fun saveOrUpdateDraft(draft: SubmissionDraft) {
        deleteDraftByCategory(draft.category)
        saveDraft(draft)
    }
}

@Database(entities = [SubmissionDraft::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun submissionDraftDao(): SubmissionDraftDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "iaa_app_database",
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
