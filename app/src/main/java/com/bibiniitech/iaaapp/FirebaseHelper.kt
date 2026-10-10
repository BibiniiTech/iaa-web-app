package com.bibiniitech.iaaapp

import android.content.Context
import android.net.Uri
import androidx.annotation.Keep
import com.bibiniitech.iaaapp.ui.screens.SubmissionDeadline
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

@Keep
data class SubmissionRecord(
    val id: String = "",
    val userId: String = "",
    val category: String = "",
    val institutionType: String = "",
    val institution: String = "",
    val region: String = "",
    val senderName: String = "",
    val senderEmail: String = "",
    val timestamp: Long = 0,
    val fileNames: List<String> = emptyList(),
    val reportName: String = "",
)

@Keep
data class UserProfile(
    val uid: String = "",
    val firstName: String = "",
    val surname: String = "",
    val email: String = "",
    val institution: String = "",
    val phone: String = "",
    val role: String = "user",
    @get:PropertyName("profileComplete")
    @set:PropertyName("profileComplete")
    var isProfileComplete: Boolean = false
) {
    @PropertyName("isProfileComplete")
    fun getLegacyProfileComplete(): Boolean = isProfileComplete

    @PropertyName("isProfileComplete")
    fun setLegacyProfileComplete(value: Boolean) {
        if (value) isProfileComplete = true
    }
}

@Keep
data class Seminar(
    val id: String = "",
    val topic: String = "",
    val date: String = "",
    val time: String = "",
    val location: String = "",
    val fee: String = "",
    val registrationUrl: String = ""
)

@Keep
data class ContactConfig(
    val address: String = "Tumu Avenue, Kanda Accra - Ghana\nDigital Address: GV-002-6511",
    val phone: String = "+233 (0) 362 196 941",
    val emails: String = "info@iaa.gov.gh\niaamails@iaa.gov.gh",
    val specificDesks: String = "CCCC: +233 (0) 362 196 941\nNACAP: +233 (0) 362 196 941\nTrainings: +233 (0) 362 196 941",
    val locationQuery: String = "Internal+Audit+Agency+Tumu+Avenue+Kanda+Accra",
    val facebookUrl: String = "https://web.facebook.com/internalauditagency/?_rdc=1&_rdr#",
    val instagramUrl: String = "https://www.instagram.com/iaa.gov.gh/",
    val xUrl: String = "https://x.com/IAA_Ghana"
)

@Keep
data class WelcomeConfig(
    val title: String = "Welcome!",
    val description: String = "This app aims to ease the submission of the various reports expected from Internal Audit Functions to the IAA and to make resources readily available and accessible to Internal Auditors and Stakeholders."
)

@Keep
data class NoticeConfig(
    val title: String = "Notices",
    val content: String = "The IAA has relocated its office premises to Tumu Avenue, Kanda Accra - Ghana with Digital Address: GV-002-6511"
)

@Keep
data class DbDocument(
    val id: String = "",
    val name: String = "",
    val category: String = "", // legislations, guidelines, templates, checklists
    val downloadUrl: String = "",
    val timestamp: Long = 0
)

@Keep
data class VotingOption(
    val id: String = "",
    val text: String = "",
    val imageUrl: String = ""
)

@Keep
data class VotingCategory(
    val id: String = "",
    val name: String = "",
    val options: List<VotingOption> = emptyList(),
    val allowMultiple: Boolean = false,
    val allowedVoterEmails: List<String> = emptyList()
)

@Keep
data class VotingConfig(
    @get:PropertyName("visible")
    @set:PropertyName("visible")
    var isVisible: Boolean = false,
    val header: String = "",
    val categories: List<VotingCategory> = emptyList()
)

@Keep
data class VoteRecord(
    val id: String = "",
    val userId: String = "",
    val firstName: String = "",
    val surname: String = "",
    val email: String = "",
    val phone: String = "",
    val institutionType: String = "",
    val institution: String = "",
    val region: String = "",
    val selections: Map<String, List<String>> = emptyMap(), // categoryId -> list of optionIds
    val timestamp: Any? = null
) {
    val timestampLong: Long
        get() = when (val t = timestamp) {
            is Long -> t
            is Double -> t.toLong()
            is Timestamp -> t.toDate().time
            is java.util.Date -> t.time
            else -> 0L
        }
}

@Keep
data class VotingResults(
    val categoryTotals: Map<String, Int> = emptyMap(),
    val optionCounts: Map<String, Int> = emptyMap()
)

object FirebaseHelper {
    private val auth: FirebaseAuth? by lazy {
        try { FirebaseAuth.getInstance() } catch (_: Throwable) { null }
    }
    
    private val db: FirebaseFirestore? by lazy {
        try { FirebaseFirestore.getInstance() } catch (_: Throwable) { null }
    }

    private val storage: FirebaseStorage? by lazy {
        try { FirebaseStorage.getInstance() } catch (_: Throwable) { null }
    }

    // Mock State for when Firebase is not connected or initialized
    private var isMockMode = true
    private var mockCurrentUser: UserProfile? = null
    private val mockUsersDb = mutableMapOf<String, UserProfile>()
    private var mockWelcomeConfig = WelcomeConfig()
    private var mockNoticeConfig = NoticeConfig()
    private var mockContactConfig = ContactConfig()
    private var mockShowDisclaimer = true
    private var mockShowPfmLeague = true
    
    private val mockDeadlines = mutableListOf<SubmissionDeadline>().apply {
        addAll(SubmissionDeadline.defaultDeadlines)
    }

    private val mockSeminars = mutableListOf(
        Seminar("1", "Leveraging the IIA Competency Framework to Build an Effective Internal Audit Function", "2026-03-26", "02:00 PM - 04:00 PM", "Online via Zoom", "Free", "https://us06web.zoom.us/meeting/register/CH2P5vejT7Gkf5VyGwwIbA"),
        Seminar("2", "Data Analytics for Auditors", "2026-11-12", "01:00 PM - 05:00 PM", "Online (Zoom)", "GH₵ 1,200.00", "https://forms.gle/example2")
    )

    private val mockDocuments = mutableListOf<DbDocument>()
    private var mockMdaPfmJson = ""
    private var mockMmdaPfmJson = ""
    
    private var mockVotingConfig = VotingConfig()
    private val mockVotes = mutableListOf<VoteRecord>()
    private val mockVotingResults = mutableMapOf<String, Int>()
    
    // In-memory recipient email overrides for Mock mode
    private var mockRecipientEmailsJson = ""

    private val _syncToken = MutableStateFlow(0L)
    val syncToken: StateFlow<Long> = _syncToken.asStateFlow()
    private var syncListenerRegistration: ListenerRegistration? = null

    private val _authState = MutableStateFlow<UserProfile?>(null)
    val authState: StateFlow<UserProfile?> = _authState.asStateFlow()

    // In-memory caching layers
    private var cachedUserProfile: UserProfile? = null
    private var cachedWelcome: WelcomeConfig? = null
    private var cachedNotice: NoticeConfig? = null
    private var cachedContact: ContactConfig? = null
    private var cachedRecipientEmailsJson: String? = null
    private var cachedDeadlines: List<SubmissionDeadline>? = null
    private var cachedSeminars: List<Seminar>? = null
    private val cachedPfmLeague = mutableMapOf<String, String>()
    private var cachedDocuments: List<DbDocument>? = null
    private var cachedVotingConfig: VotingConfig? = null
    private var cachedVotingResults: VotingResults? = null
    private var cachedVotes: List<VoteRecord>? = null
    private var cachedSubmissions: List<SubmissionRecord>? = null
    private val cachedHasVotedMap = mutableMapOf<String, Boolean>()
    private var cachedShowDisclaimer: Boolean? = null
    private var cachedShowPfmLeague: Boolean? = null
    private var lastKnownSyncTimestamp: Long = -1L
    private val profileMutex = Mutex()
    private val documentsMutex = Mutex()

    init {
        // Automatically check if Firebase is operational, otherwise default to Mock
        isMockMode = (auth == null || db == null || storage == null)

        // Auth state listener for Firebase
        auth?.addAuthStateListener { firebaseAuth: FirebaseAuth ->
            CoroutineScope(Dispatchers.IO).launch {
                val currentUser = firebaseAuth.currentUser
                val user = if (currentUser != null) {
                    cachedUserProfile?.takeIf { it.uid == currentUser.uid } ?: getCurrentUser()
                } else {
                    cachedUserProfile = null
                    cachedHasVotedMap.clear()
                    null
                }
                _authState.value = user
            }
        }

        // If in Mock Mode, use mockCurrentUser
        if (isMockMode) {
            _authState.value = mockCurrentUser
        }

    }

    private fun clearCache() {
        cachedWelcome = null
        cachedNotice = null
        cachedContact = null
        cachedRecipientEmailsJson = null
        cachedDeadlines = null
        cachedSeminars = null
        cachedPfmLeague.clear()
        cachedDocuments = null
        cachedVotingConfig = null
        cachedVotingResults = null
        cachedShowDisclaimer = null
        cachedShowPfmLeague = null
    }

    /**
     * Refreshes only the local client's in-memory cache and triggers a UI re-fetch
     * without writing to Firestore `config/sync_state` (preventing global read storms).
     */
    fun refreshLocalCache() {
        clearCache()
        _syncToken.value = System.currentTimeMillis()
    }

    fun startSyncListener() {
        if (isMockMode) return
        if (syncListenerRegistration != null) return
        try {
            syncListenerRegistration = db?.collection("config")?.document("sync_state")
                ?.addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        val lastUpdated = snapshot.getLong("lastUpdated") ?: 0L
                        if (lastKnownSyncTimestamp == -1L) {
                            // First snapshot on app startup: record baseline timestamp without wiping startup cache
                            lastKnownSyncTimestamp = lastUpdated
                        } else if (lastUpdated != lastKnownSyncTimestamp) {
                            lastKnownSyncTimestamp = lastUpdated
                            clearCache()
                            _syncToken.value = lastUpdated
                        }
                    }
                }
        } catch (_: Exception) {}
    }

    fun stopSyncListener() {
        syncListenerRegistration?.remove()
        syncListenerRegistration = null
    }

    suspend fun triggerSync(): Boolean {
        val timestamp = System.currentTimeMillis()
        lastKnownSyncTimestamp = timestamp
        clearCache()
        _syncToken.value = timestamp
        if (isMockMode) {
            return true
        }
        return try {
            db?.collection("config")?.document("sync_state")
                ?.set(mapOf("lastUpdated" to timestamp))?.await()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun setMockMode(enabled: Boolean) {
        isMockMode = enabled
        if (!enabled) {
            // If trying to disable mock mode (enable real Firebase), check if actually available
            isMockMode = (auth == null || db == null || storage == null)
        }
    }

    fun isMockEnabled(): Boolean = isMockMode

    // --- AUTHENTICATION ---
    
    fun getCurrentUser(): UserProfile? {
        if (isMockMode) return mockCurrentUser
        val firebaseUser = auth?.currentUser ?: return null
        cachedUserProfile?.takeIf { it.uid == firebaseUser.uid }?.let { return it }
        // Return a basic profile
        return UserProfile(
            uid = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            firstName = firebaseUser.displayName?.split(" ")?.firstOrNull() ?: "User"
        )
    }

    suspend fun fetchUserProfile(uid: String, forceRefresh: Boolean = false): UserProfile? {
        if (isMockMode) return mockUsersDb[uid] ?: mockCurrentUser
        if (!forceRefresh) {
            cachedUserProfile?.takeIf { it.uid == uid }?.let { return it }
        }
        return profileMutex.withLock {
            if (!forceRefresh) {
                cachedUserProfile?.takeIf { it.uid == uid }?.let { return@withLock it }
            }
            try {
                val doc = db?.collection("users")?.document(uid)?.get()?.await() ?: return@withLock null
                if (!doc.exists()) return@withLock null
                val profile = doc.toObject(UserProfile::class.java)
                if (profile != null) {
                    val complete = doc.getBoolean("profileComplete")
                        ?: doc.getBoolean("isProfileComplete")
                        ?: profile.isProfileComplete
                    profile.isProfileComplete = complete
                    cachedUserProfile = profile
                    if (auth?.currentUser?.uid == uid) {
                        _authState.value = profile
                    }
                }
                profile
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun signIn(email: String, password: String): Boolean {
        if (isMockMode) {
            val user = if (email.lowercase().startsWith("admin")) {
                UserProfile("mock_admin_123", "Admin", "User", email, "IAA Head Office", "0000000000", "admin", isProfileComplete = true)
            } else {
                UserProfile("mock_user_123", "Regular", "User", email, "Ministry of Finance", "0000000000", "user", isProfileComplete = true)
            }
            mockCurrentUser = user
            _authState.value = user
            return true
        }
        return try {
            auth?.signInWithEmailAndPassword(email, password)?.await()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Boolean {
        if (isMockMode) return true
        return try {
            auth?.sendPasswordResetEmail(email)?.await()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun signUp(email: String, password: String, firstName: String, surname: String, institution: String, phone: String): Boolean {
        if (isMockMode) {
            val role = if (email.lowercase().contains("admin")) "admin" else "user"
            val user = UserProfile("mock_user_" + System.currentTimeMillis(), firstName, surname, email, institution, phone, role, isProfileComplete = true)
            mockCurrentUser = user
            _authState.value = user
            return true
        }
        return try {
            val result = auth?.createUserWithEmailAndPassword(email, password)?.await()
            val firebaseUser = result?.user
            if (firebaseUser != null) {
                // Save user profile to Firestore with profile completion flag set
                val profile = UserProfile(
                    uid = firebaseUser.uid,
                    firstName = firstName,
                    surname = surname,
                    email = email,
                    institution = institution,
                    phone = phone,
                    role = "user", // Defaults to normal user. Admin status set manually in Firebase Console
                    isProfileComplete = true
                )
                db?.collection("users")?.document(firebaseUser.uid)?.set(profile)?.await()
                cachedUserProfile = profile
                _authState.value = profile
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun signOut() {
        cachedUserProfile = null
        cachedHasVotedMap.clear()
        if (isMockMode) {
            mockCurrentUser = null
            _authState.value = null
            return
        }
        auth?.signOut()
    }

    suspend fun updateUserProfile(profile: UserProfile): Boolean {
        if (isMockMode) {
            mockCurrentUser = profile
            _authState.value = profile
            mockUsersDb[profile.uid] = profile
            return true
        }
        return try {
            db?.collection("users")?.document(profile.uid)?.set(profile)?.await()
            cachedUserProfile = profile
            _authState.value = profile
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun isProfileComplete(uid: String): Boolean {
        val profile = fetchUserProfile(uid)
        return profile != null && (profile.isProfileComplete || (
                profile.firstName.trim().isNotEmpty() &&
                profile.surname.trim().isNotEmpty() &&
                profile.phone.trim().isNotEmpty() &&
                profile.institution.trim().isNotEmpty()
        ))
    }

    suspend fun signInWithGoogleMock(email: String, displayName: String): UserProfile {
        val uid = "mock_google_" + email.hashCode()
        val existing = mockUsersDb[uid]
        val names = displayName.split(" ")
        val firstName = names.getOrNull(0) ?: "Google"
        val surname = names.drop(1).joinToString(" ")
        
        val profile = existing ?: UserProfile(
            uid = uid,
            firstName = firstName,
            surname = surname,
            email = email,
            institution = "", // empty to trigger profile completion
            phone = "", // empty to trigger profile completion
            role = "user"
        )
        mockCurrentUser = profile
        _authState.value = profile
        mockUsersDb[uid] = profile
        return profile
    }

    suspend fun signInWithGoogleCredential(idToken: String): Boolean {
        if (isMockMode) return true
        val currentAuth = auth ?: return false
        val currentDb = db ?: return false
        return try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val result = currentAuth.signInWithCredential(credential).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                val existing = fetchUserProfile(firebaseUser.uid)
                if (existing == null) {
                    val names = (firebaseUser.displayName ?: "").split(" ")
                    val firstName = names.getOrNull(0) ?: "Google"
                    val surname = names.drop(1).joinToString(" ")
                    val profile = UserProfile(
                        uid = firebaseUser.uid,
                        firstName = firstName,
                        surname = surname,
                        email = firebaseUser.email ?: "",
                        institution = "",
                        phone = "",
                        role = "user",
                        isProfileComplete = false
                    )
                    currentDb.collection("users").document(firebaseUser.uid).set(profile).await()
                    cachedUserProfile = profile
                    _authState.value = profile
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseHelper", "Google Sign-In failed", e)
            false
        }
    }

    suspend fun deleteUserAccount(): Boolean {
        cachedUserProfile = null
        cachedHasVotedMap.clear()
        if (isMockMode) {
            mockCurrentUser = null
            _authState.value = null
            return true
        }
        return try {
            val user = auth?.currentUser ?: return false
            // Delete from Firestore
            db?.collection("users")?.document(user.uid)?.delete()?.await()
            // Delete from Auth (Note: may require recent login for security)
            user.delete().await()
            _authState.value = null
            true
        } catch (_: Exception) {
            false
        }
    }

    // --- HOME CONFIGURATION ---

    suspend fun getWelcomeConfig(): WelcomeConfig {
        if (isMockMode) return mockWelcomeConfig
        cachedWelcome?.let { return it }
        return try {
            val result = db?.collection("config")?.document("welcome_note")?.get()?.await()?.toObject(WelcomeConfig::class.java)
                ?: WelcomeConfig()
            cachedWelcome = result
            result
        } catch (_: Exception) {
            WelcomeConfig()
        }
    }

    suspend fun saveWelcomeConfig(config: WelcomeConfig, sync: Boolean = true): Boolean {
        if (isMockMode) {
            mockWelcomeConfig = config
            if (sync) triggerSync()
            return true
        }
        val currentDb = db ?: return false
        return try {
            currentDb.collection("config").document("welcome_note").set(config).await()
            cachedWelcome = config
            if (sync) triggerSync() else true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getNoticeConfig(): NoticeConfig {
        if (isMockMode) return mockNoticeConfig
        cachedNotice?.let { return it }
        return try {
            val result = db?.collection("config")?.document("notices")?.get()?.await()?.toObject(NoticeConfig::class.java)
                ?: NoticeConfig()
            cachedNotice = result
            result
        } catch (_: Exception) {
            NoticeConfig()
        }
    }

    suspend fun saveNoticeConfig(config: NoticeConfig, sync: Boolean = true): Boolean {
        if (isMockMode) {
            mockNoticeConfig = config
            if (sync) triggerSync()
            return true
        }
        val currentDb = db ?: return false
        return try {
            currentDb.collection("config").document("notices").set(config).await()
            cachedNotice = config
            if (sync) triggerSync() else true
        } catch (_: Exception) {
            false
        }
    }

    // --- DISCLAIMER CONFIGURATION ---

    suspend fun getShowDisclaimer(): Boolean {
        if (isMockMode) return mockShowDisclaimer
        cachedShowDisclaimer?.let { return it }
        return try {
            val doc = db?.collection("config")?.document("show_disclaimer")?.get()?.await()
            val result = doc?.getBoolean("show") ?: true
            cachedShowDisclaimer = result
            result
        } catch (_: Exception) {
            true
        }
    }

    suspend fun saveShowDisclaimer(show: Boolean, sync: Boolean = true): Boolean {
        if (isMockMode) {
            mockShowDisclaimer = show
            if (sync) triggerSync()
            return true
        }
        val currentDb = db ?: return false
        return try {
            currentDb.collection("config").document("show_disclaimer").set(mapOf("show" to show)).await()
            cachedShowDisclaimer = show
            if (sync) triggerSync()
            true
        } catch (_: Exception) {
            false
        }
    }

    // --- PFM LEAGUE VISIBILITY ---

    suspend fun getShowPfmLeague(): Boolean {
        if (isMockMode) return mockShowPfmLeague
        cachedShowPfmLeague?.let { return it }
        return try {
            val doc = db?.collection("config")?.document("show_pfm_league")?.get()?.await()
            val result = doc?.getBoolean("show") ?: true
            cachedShowPfmLeague = result
            result
        } catch (_: Exception) {
            true
        }
    }

    suspend fun saveShowPfmLeague(show: Boolean, sync: Boolean = true): Boolean {
        if (isMockMode) {
            mockShowPfmLeague = show
            if (sync) triggerSync()
            return true
        }
        val currentDb = db ?: return false
        return try {
            currentDb.collection("config").document("show_pfm_league").set(mapOf("show" to show)).await()
            cachedShowPfmLeague = show
            if (sync) triggerSync()
            true
        } catch (_: Exception) {
            false
        }
    }

    // --- CONTACT CONFIGURATION ---

    suspend fun getContactConfig(): ContactConfig {
        if (isMockMode) return mockContactConfig
        cachedContact?.let { return it }
        return try {
            val result = db?.collection("config")?.document("contact_info")?.get()?.await()?.toObject(ContactConfig::class.java)
                ?: ContactConfig()
            cachedContact = result
            result
        } catch (_: Exception) {
            ContactConfig()
        }
    }

    suspend fun saveContactConfig(config: ContactConfig, sync: Boolean = true): Boolean {
        if (isMockMode) {
            mockContactConfig = config
            if (sync) triggerSync()
            return true
        }
        val currentDb = db ?: return false
        return try {
            currentDb.collection("config").document("contact_info").set(config).await()
            cachedContact = config
            if (sync) triggerSync() else true
        } catch (_: Exception) {
            false
        }
    }

    // --- RECIPIENT EMAILS CONFIGURATION ---

    suspend fun getRecipientEmailsJson(): String {
        if (isMockMode) return mockRecipientEmailsJson
        cachedRecipientEmailsJson?.let { return it }
        return try {
            val doc = db?.collection("config")?.document("recipient_emails")?.get()?.await()
            val result = doc?.getString("json") ?: ""
            cachedRecipientEmailsJson = result
            result
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun ensureRecipientEmailsLoaded() {
        val json = getRecipientEmailsJson()
        if (json.isNotEmpty()) {
            CloudEmailManager.updateFirestoreConfig(json)
        }
    }

    suspend fun saveRecipientEmailsJson(json: String): Boolean {
        if (isMockMode) {
            mockRecipientEmailsJson = json
            CloudEmailManager.updateFirestoreConfig(json)
            triggerSync()
            return true
        }
        val currentDb = db ?: return false
        return try {
            currentDb.collection("config").document("recipient_emails").set(mapOf("json" to json)).await()
            cachedRecipientEmailsJson = json
            CloudEmailManager.updateFirestoreConfig(json)
            triggerSync()
        } catch (_: Exception) {
            false
        }
    }

    // --- SUBMISSION DEADLINES ---

    suspend fun getDeadlines(): List<SubmissionDeadline> {
        if (isMockMode) return mockDeadlines.sortedBy { it.date }
        cachedDeadlines?.let { return it }
        return try {
            val doc = db?.collection("config")?.document("deadlines")?.get()?.await()
            val json = doc?.getString("json") ?: ""
            val list = if (json.isNotEmpty()) {
                val type = object : TypeToken<List<SubmissionDeadline>>() {}.type
                Gson().fromJson<List<SubmissionDeadline>>(json, type)
            } else {
                emptyList()
            }
            val result = list.ifEmpty { SubmissionDeadline.defaultDeadlines }.sortedBy { it.date }
            cachedDeadlines = result
            result
        } catch (_: Exception) {
            SubmissionDeadline.defaultDeadlines.sortedBy { it.date }
        }
    }

    suspend fun saveDeadlines(deadlines: List<SubmissionDeadline>): Boolean {
        if (isMockMode) {
            mockDeadlines.clear()
            mockDeadlines.addAll(deadlines)
            triggerSync()
            return true
        }
        val currentDb = db ?: return false
        return try {
            val json = Gson().toJson(deadlines)
            currentDb.collection("config").document("deadlines").set(mapOf("json" to json)).await()
            cachedDeadlines = deadlines.sortedBy { it.date }
            triggerSync()
        } catch (_: Exception) {
            false
        }
    }

    // --- TRAINING SEMINARS ---

    suspend fun getSeminars(): List<Seminar> {
        if (isMockMode) return mockSeminars
        cachedSeminars?.let { return it }
        return try {
            val doc = db?.collection("config")?.document("seminars")?.get()?.await()
            val json = doc?.getString("json") ?: ""
            val list = if (json.isNotEmpty()) {
                val type = object : TypeToken<List<Seminar>>() {}.type
                Gson().fromJson<List<Seminar>>(json, type)
            } else {
                emptyList()
            }
            cachedSeminars = list
            list
        } catch (_: Exception) {
            mockSeminars
        }
    }

    suspend fun saveSeminar(seminar: Seminar): Boolean {
        if (isMockMode) {
            val existingIndex = mockSeminars.indexOfFirst { it.id == seminar.id }
            if (existingIndex != -1) {
                mockSeminars[existingIndex] = seminar
            } else {
                val newId = (mockSeminars.size + 1).toString()
                mockSeminars.add(seminar.copy(id = newId))
            }
            triggerSync()
            return true
        }
        val currentDb = db ?: return false
        return try {
            val list = getSeminars().toMutableList()
            if (seminar.id.isNotEmpty()) {
                val existingIndex = list.indexOfFirst { it.id == seminar.id }
                if (existingIndex != -1) {
                    list[existingIndex] = seminar
                } else {
                    list.add(seminar)
                }
            } else {
                val newId = currentDb.collection("config").document().id
                val created = seminar.copy(id = newId)
                list.add(created)
            }
            val json = Gson().toJson(list)
            currentDb.collection("config").document("seminars").set(mapOf("json" to json)).await()
            cachedSeminars = list
            triggerSync()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteSeminar(id: String): Boolean {
        if (isMockMode) {
            mockSeminars.removeAll { it.id == id }
            triggerSync()
            return true
        }
        val currentDb = db ?: return false
        return try {
            val list = getSeminars().filter { it.id != id }
            val json = Gson().toJson(list)
            currentDb.collection("config").document("seminars").set(mapOf("json" to json)).await()
            cachedSeminars = list
            triggerSync()
        } catch (_: Exception) {
            false
        }
    }

    // --- PFM LEAGUE TABLES ---

    suspend fun getPfmLeagueData(category: String): String {
        if (isMockMode) return if (category == "mda") mockMdaPfmJson else mockMmdaPfmJson
        cachedPfmLeague[category]?.let { return it }
        return try {
            val doc = db?.collection("config")?.document("pfm_league_$category")?.get()?.await()
            val result = doc?.getString("json") ?: ""
            cachedPfmLeague[category] = result
            result
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun savePfmLeagueData(category: String, json: String, sync: Boolean = true): Boolean {
        if (isMockMode) {
            if (category == "mda") mockMdaPfmJson = json else mockMmdaPfmJson = json
            if (sync) triggerSync()
            return true
        }
        val currentDb = db ?: return false
        return try {
            currentDb.collection("config").document("pfm_league_$category").set(mapOf("json" to json)).await()
            cachedPfmLeague[category] = json
            if (sync) triggerSync() else true
        } catch (_: Exception) {
            false
        }
    }

    // --- DOCUMENT LOGISTICS (FIREBASE STORAGE) ---

    private suspend fun fetchAllDocumentsInternal(): List<DbDocument>? {
        if (isMockMode) return mockDocuments
        cachedDocuments?.let { return it }

        return documentsMutex.withLock {
            cachedDocuments?.let { return@withLock it }
            val currentDb = db ?: return@withLock null
            try {
                // Use a dedicated collection for documents to ensure concurrent uploads don't overwrite each other
                val snapshot = currentDb.collection("portal_documents")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(200)
                    .get().await()
                val list = snapshot.toObjects(DbDocument::class.java)
                cachedDocuments = list
                list
            } catch (e: Exception) {
                android.util.Log.e("FirebaseHelper", "Fetch documents failed", e)
                null
            }
        }
    }

    suspend fun getDocuments(category: String): List<DbDocument> {
        return fetchAllDocumentsInternal()?.filter { it.category == category } ?: emptyList()
    }

    suspend fun deleteStorageUrlIfPresent(url: String) {
        if (isMockMode || url.isBlank() || !url.contains("firebasestorage.googleapis.com")) return
        try {
            storage?.getReferenceFromUrl(url)?.delete()?.await()
        } catch (_: Exception) {}
    }

    suspend fun uploadDocuments(category: String, files: List<Pair<Uri, String>>, context: Context): String? {
        if (isMockMode) {
            files.forEach { (_, fileName) ->
                // In mock mode, we still replace by name for local testing sanity
                mockDocuments.removeAll { it.name == fileName && it.category == category }
                val mockUrl = "https://mock.storage.googleapis.com/$category/$fileName"
                mockDocuments.add(DbDocument("mock_doc_" + System.currentTimeMillis(), fileName, category, mockUrl, System.currentTimeMillis()))
            }
            triggerSync()
            return null // null means success
        }
        val currentDb = db ?: return "Firestore database not initialized"
        val currentStorage = storage ?: return "Firebase Storage not initialized"

        return try {
            val existingCategoryDocs = fetchAllDocumentsInternal()?.filter { it.category == category } ?: emptyList()
            val uploadTimestamp = System.currentTimeMillis()
            for ((fileUri, fileName) in files) {
                var compressedTempFile: java.io.File? = null
                try {
                    // Automatically compress uploaded PDFs, images, or Office documents before uploading
                    compressedTempFile = FileCompressionUtils.compressUriToTempFile(context, fileUri, fileName)
                    val uploadUri = compressedTempFile?.let { Uri.fromFile(it) } ?: fileUri

                    // Clean up any existing document with the same name in this category to prevent Storage & Firestore bloat
                    val duplicates = existingCategoryDocs.filter { it.name.equals(fileName, ignoreCase = true) }
                    for (oldDoc in duplicates) {
                        deleteStorageUrlIfPresent(oldDoc.downloadUrl)
                        try {
                            currentDb.collection("portal_documents").document(oldDoc.id).delete().await()
                        } catch (_: Exception) {}
                    }

                    val uniqueFileName = "${uploadTimestamp}_$fileName"
                    val storageRef = currentStorage.reference.child("documents/$category/$uniqueFileName")
                    storageRef.putFile(uploadUri).await()
                    val downloadUrl = storageRef.downloadUrl.await().toString()

                    val newId = currentDb.collection("portal_documents").document().id
                    val dbDoc = DbDocument(newId, fileName, category, downloadUrl, uploadTimestamp)
                    currentDb.collection("portal_documents").document(newId).set(dbDoc).await()
                } finally {
                    try {
                        compressedTempFile?.delete()
                    } catch (_: Exception) {}
                }
            }
            
            // Explicitly clear cache and trigger global sync
            cachedDocuments = null
            triggerSync()
            null // null means success
        } catch (e: Exception) {
            android.util.Log.e("FirebaseHelper", "Upload failed", e)
            e.localizedMessage ?: e.message ?: "Unknown upload exception"
        }
    }

    suspend fun uploadDocument(category: String, fileName: String, fileUri: Uri, context: Context): Boolean {
        return uploadDocuments(category, listOf(fileUri to fileName), context) == null
    }

    suspend fun deleteDocument(doc: DbDocument): Boolean {
        if (isMockMode) {
            mockDocuments.removeAll { it.id == doc.id }
            triggerSync()
            return true
        }
        val currentDb = db ?: return false
        val currentStorage = storage ?: return false

        return try {
            // Delete from Storage if it exists
            try {
                val storageRef = currentStorage.getReferenceFromUrl(doc.downloadUrl)
                storageRef.delete().await()
            } catch (_: Exception) {}

            // Delete from dedicated Firestore collection
            currentDb.collection("portal_documents").document(doc.id).delete().await()
            
            cachedDocuments = null
            triggerSync()
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseHelper", "Delete failed", e)
            false
        }
    }

    // --- LOG SUBMISSION ---
    suspend fun logSubmission(
        category: String,
        institutionType: String,
        institution: String,
        region: String,
        senderName: String,
        senderEmail: String,
        fileNames: List<String>,
        reportName: String = "",
        userId: String = ""
    ): Boolean {
        if (isMockMode) return true
        val currentDb = db ?: return false
        val uid = userId.ifEmpty { auth?.currentUser?.uid ?: "" }
        return try {
            val submission = mapOf(
                "userId" to uid,
                "category" to category,
                "institutionType" to institutionType,
                "institution" to institution,
                "region" to region,
                "senderName" to senderName,
                "senderEmail" to senderEmail,
                "timestamp" to System.currentTimeMillis(),
                "fileNames" to fileNames,
                "reportName" to reportName
            )
            currentDb.collection("submissions").add(submission).await()
            cachedSubmissions = null
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseHelper", "Submission logging failed", e)
            false
        }
    }

    suspend fun getSubmissions(limit: Long = 50, forceRefresh: Boolean = false): List<SubmissionRecord> {
        if (isMockMode) return emptyList()
        if (!forceRefresh) {
            cachedSubmissions?.let { return it }
        }
        val currentDb = db ?: return emptyList()
        return try {
            val snapshot = currentDb.collection("submissions")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)
                .get().await()
            val list = snapshot.documents.map { doc ->
                SubmissionRecord(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    category = doc.getString("category") ?: "",
                    institutionType = doc.getString("institutionType") ?: "",
                    institution = doc.getString("institution") ?: "",
                    region = doc.getString("region") ?: "",
                    senderName = doc.getString("senderName") ?: "",
                    senderEmail = doc.getString("senderEmail") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    fileNames = (doc["fileNames"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    reportName = doc.getString("reportName") ?: ""
                )
            }
            cachedSubmissions = list
            list
        } catch (e: Exception) {
            android.util.Log.e("FirebaseHelper", "Fetch submissions failed", e)
            emptyList()
        }
    }

    // --- VOTING SYSTEM ---

    suspend fun getVotingConfig(): VotingConfig {
        if (isMockMode) return mockVotingConfig
        cachedVotingConfig?.let { return it }
        return try {
            val doc = db?.collection("config")?.document("voting_config")?.get()?.await()
            val result = doc?.toObject(VotingConfig::class.java) ?: VotingConfig()
            cachedVotingConfig = result
            result
        } catch (_: Exception) {
            VotingConfig()
        }
    }

    suspend fun saveVotingConfig(config: VotingConfig): Boolean {
        if (isMockMode) {
            mockVotingConfig = config
            triggerSync()
            return true
        }
        val currentDb = db ?: return false
        return try {
            // Clean up any orphaned voting option images that were removed or replaced
            val previousUrls = cachedVotingConfig?.categories
                ?.flatMap { it.options }
                ?.map { it.imageUrl }
                ?.filter { it.isNotBlank() }
                ?.toSet() ?: emptySet()
            val newUrls = config.categories
                .flatMap { it.options }
                .map { it.imageUrl }
                .filter { it.isNotBlank() }
                .toSet()
            for (removedUrl in (previousUrls - newUrls)) {
                deleteStorageUrlIfPresent(removedUrl)
            }

            currentDb.collection("config").document("voting_config").set(config).await()
            cachedVotingConfig = config
            triggerSync()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun submitVote(vote: VoteRecord): Boolean {
        if (isMockMode) {
            mockVotes.add(vote.copy(id = "mock_vote_" + System.currentTimeMillis(), timestamp = System.currentTimeMillis()))
            for ((categoryId, optionIds) in vote.selections) {
                if (optionIds.isNotEmpty()) {
                    mockVotingResults["cat_$categoryId"] = (mockVotingResults["cat_$categoryId"] ?: 0) + 1
                    for (optionId in optionIds) {
                        mockVotingResults["opt_$optionId"] = (mockVotingResults["opt_$optionId"] ?: 0) + 1
                    }
                }
            }
            return true
        }
        val currentDb = db ?: return false
        val voterId = vote.userId.ifEmpty { auth?.currentUser?.uid ?: "" }
        if (voterId.isEmpty()) return false
        return try {
            val voteWithId = vote.copy(id = voterId, userId = voterId, timestamp = FieldValue.serverTimestamp())
            val batch = currentDb.batch()
            val voteDocRef = currentDb.collection("votes").document(voterId)
            batch.set(voteDocRef, voteWithId)

            val resultsDocRef = currentDb.collection("config").document("voting_results")
            val updates = mutableMapOf<String, Any>()
            for ((categoryId, optionIds) in vote.selections) {
                if (optionIds.isNotEmpty()) {
                    updates["cat_$categoryId"] = FieldValue.increment(1)
                    for (optionId in optionIds) {
                        updates["opt_$optionId"] = FieldValue.increment(1)
                    }
                }
            }
            if (updates.isNotEmpty()) {
                batch.set(resultsDocRef, updates, SetOptions.merge())
            }
            batch.commit().await()
            cachedHasVotedMap[voterId] = true
            cachedVotingResults = null
            cachedVotes = null
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseHelper", "Vote submission failed", e)
            false
        }
    }

    suspend fun hasUserVoted(userId: String): Boolean {
        if (isMockMode) return mockVotes.any { it.userId == userId }
        if (userId.isBlank()) return false
        cachedHasVotedMap[userId]?.let { return it }
        val currentDb = db ?: return false
        return try {
            val doc = currentDb.collection("votes").document(userId).get().await()
            val voted = doc != null && doc.exists()
            cachedHasVotedMap[userId] = voted
            voted
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getVotes(forceRefresh: Boolean = false): List<VoteRecord> {
        if (isMockMode) {
            return mockVotes.sortedBy { it.timestampLong }.associateBy { it.userId }.values.toList()
        }
        if (!forceRefresh) {
            cachedVotes?.let { return it }
        }
        val currentDb = db ?: return emptyList()
        return try {
            val snapshot = currentDb.collection("votes").get().await()
            val rawVotes = snapshot.toObjects(VoteRecord::class.java)

            // Deduplicate by User ID: ensure only the most recent vote per user is counted
            val uniqueVotes = rawVotes.sortedBy { it.timestampLong }
                .associateBy { it.userId }
                .values
                .toList()

            cachedVotes = uniqueVotes
            android.util.Log.d("FirebaseHelper", "Votes processed - Raw count: ${snapshot.size()}, Unique count: ${uniqueVotes.size}")

            uniqueVotes
        } catch (e: Exception) {
            android.util.Log.e("FirebaseHelper", "Fetch votes failed", e)
            emptyList()
        }
    }

    fun listenToVotes(onVotesChanged: (List<VoteRecord>) -> Unit): ListenerRegistration? {
        if (isMockMode) {
            onVotesChanged(mockVotes.sortedBy { it.timestampLong }.associateBy { it.userId }.values.toList())
            return null
        }
        val currentDb = db ?: return null
        return try {
            currentDb.collection("votes")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FirebaseHelper", "Listen to votes failed", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val rawVotes = snapshot.toObjects(VoteRecord::class.java)
                        val uniqueVotes = rawVotes.sortedBy { it.timestampLong }
                            .associateBy { it.userId }
                            .values
                            .toList()
                        cachedVotes = uniqueVotes
                        onVotesChanged(uniqueVotes)
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseHelper", "Error setting up votes listener", e)
            null
        }
    }

    fun listenToVotingResults(onResultsChanged: (VotingResults) -> Unit): ListenerRegistration? {
        if (isMockMode) {
            val catTotals = mutableMapOf<String, Int>()
            val optCounts = mutableMapOf<String, Int>()
            mockVotingResults.forEach { (key, count) ->
                if (key.startsWith("cat_")) catTotals[key.removePrefix("cat_")] = count
                else if (key.startsWith("opt_")) optCounts[key.removePrefix("opt_")] = count
            }
            onResultsChanged(VotingResults(categoryTotals = catTotals, optionCounts = optCounts))
            return null
        }
        val currentDb = db ?: return null
        return try {
            currentDb.collection("config").document("voting_results")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FirebaseHelper", "Listen to voting results failed", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val catTotals = mutableMapOf<String, Int>()
                        val optCounts = mutableMapOf<String, Int>()
                        snapshot.data?.forEach { (key, value) ->
                            val count = (value as? Number)?.toInt() ?: 0
                            if (key.startsWith("cat_")) catTotals[key.removePrefix("cat_")] = count
                            else if (key.startsWith("opt_")) optCounts[key.removePrefix("opt_")] = count
                        }
                        val results = VotingResults(categoryTotals = catTotals, optionCounts = optCounts)
                        cachedVotingResults = results
                        onResultsChanged(results)
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseHelper", "Error setting up voting results listener", e)
            null
        }
    }

    suspend fun getVotingResults(forceRefresh: Boolean = false): VotingResults {
        if (isMockMode) {
            val catTotals = mutableMapOf<String, Int>()
            val optCounts = mutableMapOf<String, Int>()
            mockVotingResults.forEach { (key, count) ->
                if (key.startsWith("cat_")) catTotals[key.removePrefix("cat_")] = count
                else if (key.startsWith("opt_")) optCounts[key.removePrefix("opt_")] = count
            }
            return VotingResults(categoryTotals = catTotals, optionCounts = optCounts)
        }
        if (!forceRefresh) {
            cachedVotingResults?.let { return it }
        }
        val currentDb = db ?: return VotingResults()
        return try {
            val snapshot = currentDb.collection("config").document("voting_results").get().await()
            val catTotals = mutableMapOf<String, Int>()
            val optCounts = mutableMapOf<String, Int>()
            snapshot.data?.forEach { (key, value) ->
                val count = (value as? Number)?.toInt() ?: 0
                if (key.startsWith("cat_")) catTotals[key.removePrefix("cat_")] = count
                else if (key.startsWith("opt_")) optCounts[key.removePrefix("opt_")] = count
            }
            val results = VotingResults(categoryTotals = catTotals, optionCounts = optCounts)
            cachedVotingResults = results
            results
        } catch (e: Exception) {
            android.util.Log.e("FirebaseHelper", "Fetch voting results failed", e)
            VotingResults()
        }
    }

    suspend fun recalculateVotingResults(): Boolean {
        if (isMockMode) return true
        val currentDb = db ?: return false
        return try {
            val uniqueVotes = getVotes()
            val catTotals = mutableMapOf<String, Any>()
            for (vote in uniqueVotes) {
                for ((catId, optIds) in vote.selections) {
                    if (optIds.isNotEmpty()) {
                        catTotals["cat_$catId"] = ((catTotals["cat_$catId"] as? Int) ?: 0) + 1
                        for (optId in optIds) {
                            catTotals["opt_$optId"] = ((catTotals["opt_$optId"] as? Int) ?: 0) + 1
                        }
                    }
                }
            }
            currentDb.collection("config").document("voting_results").set(catTotals).await()
            cachedVotingResults = null
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseHelper", "Recalculate voting results failed", e)
            false
        }
    }

    suspend fun uploadVotingImage(uri: Uri, context: Context): String? {
        if (isMockMode) return "https://mock.storage.googleapis.com/voting/${System.currentTimeMillis()}.jpg"
        val currentStorage = storage ?: return null
        var compressedFile: java.io.File? = null
        return try {
            // Automatically downscale and compress voting image before uploading
            compressedFile = FileCompressionUtils.compressVotingImageToFile(context, uri)
            val uploadUri = compressedFile?.let { Uri.fromFile(it) } ?: uri
            val ext = if (compressedFile != null) "jpg" else "png"
            val fileName = "${System.currentTimeMillis()}.$ext"
            val ref = currentStorage.reference.child("voting/$fileName")

            ref.putFile(uploadUri).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseHelper", "Voting image upload failed", e)
            null
        } finally {
            try {
                compressedFile?.delete()
            } catch (_: Exception) {}
        }
    }

}
