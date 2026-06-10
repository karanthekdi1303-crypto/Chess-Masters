package com.example.chess.data

import android.os.Build
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// Firestore REST Models
@JsonClass(generateAdapter = true)
data class FirestoreValue(
    @Json(name = "stringValue") val stringValue: String? = null,
    @Json(name = "integerValue") val integerValue: String? = null,
    @Json(name = "booleanValue") val booleanValue: Boolean? = null,
    @Json(name = "doubleValue") val doubleValue: Double? = null
)

@JsonClass(generateAdapter = true)
data class FirestoreDocument(
    @Json(name = "name") val name: String? = null,
    @Json(name = "fields") val fields: Map<String, FirestoreValue>? = null,
    @Json(name = "createTime") val createTime: String? = null,
    @Json(name = "updateTime") val updateTime: String? = null
)

@JsonClass(generateAdapter = true)
data class FirestoreCollectionResponse(
    @Json(name = "documents") val documents: List<FirestoreDocument>? = null
)

// Firebase Auth REST Models
@JsonClass(generateAdapter = true)
data class FirebaseAuthRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "returnSecureToken") val returnSecureToken: Boolean = true
)

@JsonClass(generateAdapter = true)
data class FirebaseAuthResponse(
    @Json(name = "localId") val localId: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "idToken") val idToken: String? = null,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "registered") val registered: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class FirebaseUserLookupRequest(
    @Json(name = "idToken") val idToken: String
)

@JsonClass(generateAdapter = true)
data class FirebaseUserLookupResponse(
    @Json(name = "users") val users: List<FirebaseUser>? = null
)

@JsonClass(generateAdapter = true)
data class FirebaseUser(
    @Json(name = "localId") val localId: String,
    @Json(name = "email") val email: String?,
    @Json(name = "displayName") val displayName: String?,
    @Json(name = "createdAt") val createdAt: String?,
    @Json(name = "emailVerified") val emailVerified: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class FirebaseOobCodeRequest(
    @Json(name = "requestType") val requestType: String,
    @Json(name = "email") val email: String? = null,
    @Json(name = "idToken") val idToken: String? = null
)

@JsonClass(generateAdapter = true)
data class FirebaseOobCodeResponse(
    @Json(name = "email") val email: String? = null
)

interface FirebaseRestApi {
    // FirebaseAuth endpoints
    @POST("identitytoolkit/v1/accounts:signUp")
    suspend fun signUp(
        @Query("key") apiKey: String,
        @Body request: FirebaseAuthRequest
    ): Response<FirebaseAuthResponse>

    @POST("identitytoolkit/v1/accounts:signInWithPassword")
    suspend fun signIn(
        @Query("key") apiKey: String,
        @Body request: FirebaseAuthRequest
    ): Response<FirebaseAuthResponse>

    @POST("identitytoolkit/v1/accounts:lookup")
    suspend fun lookupUser(
        @Query("key") apiKey: String,
        @Body request: FirebaseUserLookupRequest
    ): Response<FirebaseUserLookupResponse>

    @POST("identitytoolkit/v1/accounts:sendOobCode")
    suspend fun sendOobCode(
        @Query("key") apiKey: String,
        @Body request: FirebaseOobCodeRequest
    ): Response<FirebaseOobCodeResponse>

    // Firestore endpoints
    @GET("v1/projects/{projectId}/databases/(default)/documents/{collection}/{documentId}")
    suspend fun getDocument(
        @Path("projectId") projectId: String,
        @Path("collection") collection: String,
        @Path("documentId") documentId: String,
        @Query("key") apiKey: String
    ): Response<FirestoreDocument>

    @PATCH("v1/projects/{projectId}/databases/(default)/documents/{collection}/{documentId}")
    suspend fun updateDocument(
        @Path("projectId") projectId: String,
        @Path("collection") collection: String,
        @Path("documentId") documentId: String,
        @Query("key") apiKey: String,
        @Body document: FirestoreDocument
    ): Response<FirestoreDocument>

    @GET("v1/projects/{projectId}/databases/(default)/documents/{collection}")
    suspend fun getCollection(
        @Path("projectId") projectId: String,
        @Path("collection") collection: String,
        @Query("key") apiKey: String,
        @Query("pageSize") pageSize: Int = 100
    ): Response<FirestoreCollectionResponse>
}

object FirebaseService {
    // Standard Demo Firebase Setup configured default, meaning the app runs securely out of the box.
    // Users can also override this seamlessly in Settings to use their own custom Firebase Project keys.
    var currentApiKey: String = com.example.BuildConfig.FIREBASE_API_KEY.ifEmpty { "AIzaSyD-demo-placeholder-and-built-in-key" }
    var currentProjectId: String = com.example.BuildConfig.FIREBASE_PROJECT_ID.ifEmpty { "master-chess-pro" }

    private const val AUTH_ID_BASE_URL = "https://identitytoolkit.googleapis.com/"
    private const val FIRESTORE_BASE_URL = "https://firestore.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val authRetrofit = Retrofit.Builder()
        .baseUrl(AUTH_ID_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    private val firestoreRetrofit = Retrofit.Builder()
        .baseUrl(FIRESTORE_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val authApi: FirebaseRestApi by lazy { authRetrofit.create(FirebaseRestApi::class.java) }
    val firestoreApi: FirebaseRestApi by lazy { firestoreRetrofit.create(FirebaseRestApi::class.java) }

    // Helpers to convert local GameStats to FirestoreDocument type-safely and elegantly
    fun buildFirestoreDocument(stats: GameStats): FirestoreDocument {
        val fields = mutableMapOf<String, FirestoreValue>()
        fields["id"] = FirestoreValue(integerValue = stats.id.toString())
        fields["username"] = FirestoreValue(stringValue = stats.username)
        fields["fullName"] = FirestoreValue(stringValue = stats.fullName)
        fields["email"] = FirestoreValue(stringValue = stats.email)
        fields["phoneNumber"] = FirestoreValue(stringValue = stats.phoneNumber)
        fields["dob"] = FirestoreValue(stringValue = stats.dob)
        fields["playerId"] = FirestoreValue(stringValue = stats.playerId)
        fields["avatar"] = FirestoreValue(stringValue = stats.avatar)
        fields["theme"] = FirestoreValue(stringValue = stats.theme)
        fields["language"] = FirestoreValue(stringValue = stats.language)
        fields["commentaryEnabled"] = FirestoreValue(booleanValue = stats.commentaryEnabled)
        fields["gamesPlayed"] = FirestoreValue(integerValue = stats.gamesPlayed.toString())
        fields["wins"] = FirestoreValue(integerValue = stats.wins.toString())
        fields["losses"] = FirestoreValue(integerValue = stats.losses.toString())
        fields["draws"] = FirestoreValue(integerValue = stats.draws.toString())
        fields["winStreak"] = FirestoreValue(integerValue = stats.winStreak.toString())
        fields["gameHistory"] = FirestoreValue(stringValue = stats.gameHistory)
        fields["pieceStyle"] = FirestoreValue(stringValue = stats.pieceStyle)
        fields["pieceColorStyle"] = FirestoreValue(stringValue = stats.pieceColorStyle)
        
        // Calculated/Derived statistics so they are saved securely in Firestore and syncable across devices
        fields["battleScore"] = FirestoreValue(integerValue = stats.battleScore.toString())
        fields["winRate"] = FirestoreValue(doubleValue = stats.winRate.toDouble())
        fields["krRatio"] = FirestoreValue(doubleValue = stats.krRatio.toDouble())
        fields["playerTitle"] = FirestoreValue(stringValue = stats.playerTitle)
        
        fields["lastLogin"] = FirestoreValue(stringValue = stats.lastLogin)
        fields["deviceInformation"] = FirestoreValue(stringValue = stats.deviceInformation)
        fields["cloudUserId"] = FirestoreValue(stringValue = stats.cloudUserId)
        fields["creationDate"] = FirestoreValue(stringValue = stats.creationDate)
        fields["role"] = FirestoreValue(stringValue = if (stats.isDatabaseAdmin) "admin" else "user")
        fields["emailVerified"] = FirestoreValue(booleanValue = stats.emailVerified)
        fields["accountStatus"] = FirestoreValue(stringValue = stats.accountStatus)
        fields["warningLevel"] = FirestoreValue(integerValue = stats.warningLevel.toString())
        fields["fraudFlags"] = FirestoreValue(integerValue = stats.fraudFlags.toString())

        return FirestoreDocument(fields = fields)
    }

    fun parseFirestoreDocument(doc: FirestoreDocument): GameStats? {
        val fields = doc.fields ?: return null
        
        val id = fields["id"]?.integerValue?.toIntOrNull() ?: 1
        val username = fields["username"]?.stringValue ?: "Grandmaster"
        val fullName = fields["fullName"]?.stringValue ?: ""
        val email = fields["email"]?.stringValue ?: ""
        val phoneNumber = fields["phoneNumber"]?.stringValue ?: ""
        val dob = fields["dob"]?.stringValue ?: ""
        val playerId = fields["playerId"]?.stringValue ?: ""
        val avatar = fields["avatar"]?.stringValue ?: "♔"
        val theme = fields["theme"]?.stringValue ?: "Frosted Glass"
        val language = fields["language"]?.stringValue ?: "English"
        val commentaryEnabled = fields["commentaryEnabled"]?.booleanValue ?: true
        val gamesPlayed = fields["gamesPlayed"]?.integerValue?.toIntOrNull() ?: 0
        val wins = fields["wins"]?.integerValue?.toIntOrNull() ?: 0
        val losses = fields["losses"]?.integerValue?.toIntOrNull() ?: 0
        val draws = fields["draws"]?.integerValue?.toIntOrNull() ?: 0
        val winStreak = fields["winStreak"]?.integerValue?.toIntOrNull() ?: 0
        val gameHistory = fields["gameHistory"]?.stringValue ?: ""
        val pieceStyle = fields["pieceStyle"]?.stringValue ?: "Classic Outline"
        val pieceColorStyle = fields["pieceColorStyle"]?.stringValue ?: "Standard Crisp"
        val lastLogin = fields["lastLogin"]?.stringValue ?: ""
        val deviceInformation = fields["deviceInformation"]?.stringValue ?: ""
        val cloudUserId = fields["cloudUserId"]?.stringValue ?: ""
        val creationDate = fields["creationDate"]?.stringValue ?: ""
        val role = fields["role"]?.stringValue ?: "user"
        val emailVerified = fields["emailVerified"]?.booleanValue ?: false
        val accountStatus = fields["accountStatus"]?.stringValue ?: "Active"
        val warningLevel = fields["warningLevel"]?.integerValue?.toIntOrNull() ?: 0
        val fraudFlags = fields["fraudFlags"]?.integerValue?.toIntOrNull() ?: 0

        return GameStats(
            id = id,
            username = username,
            avatar = avatar,
            theme = theme,
            language = language,
            commentaryEnabled = commentaryEnabled,
            gamesPlayed = gamesPlayed,
            wins = wins,
            losses = losses,
            draws = draws,
            winStreak = winStreak,
            gameHistory = gameHistory,
            pieceStyle = pieceStyle,
            pieceColorStyle = pieceColorStyle,
            fullName = fullName,
            email = email,
            phoneNumber = phoneNumber,
            dob = dob,
            playerId = playerId,
            lastLogin = lastLogin,
            deviceInformation = deviceInformation,
            cloudSynced = true,
            cloudUserId = cloudUserId,
            creationDate = creationDate,
            isDatabaseAdmin = (role == "admin"),
            emailVerified = emailVerified,
            accountStatus = accountStatus,
            warningLevel = warningLevel,
            fraudFlags = fraudFlags
        )
    }
}
