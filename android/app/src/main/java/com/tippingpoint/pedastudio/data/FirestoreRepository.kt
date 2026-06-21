package com.tippingpoint.pedastudio.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

class FirestoreRepository(
    private val tlmCatalog: TlmResourceCatalog,
) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun uid(): String? = auth.currentUser?.uid

    suspend fun ensureCatalogSeeded(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val ref = db.collection("catalog").document("tlmResources")
            val snap = ref.get().await()
            if (!snap.exists()) {
                ref.set(
                    mapOf(
                        "resources" to tlmCatalog.toFirestoreList(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            }
            Unit
        }
    }

    suspend fun pushProfile(prefs: UserPreferences): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = uid() ?: throw IllegalStateException("Not signed in")
            db.collection("users").document(userId)
                .set(prefs.toFirestoreMap(), SetOptions.merge())
                .await()
            Unit
        }
    }

    suspend fun pullProfile(prefs: UserPreferences): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = uid() ?: throw IllegalStateException("Not signed in")
            val snap = db.collection("users").document(userId).get().await()
            if (!snap.exists()) return@runCatching false
            prefs.applyFromFirestore(snap.data ?: emptyMap())
            true
        }
    }

    suspend fun pushPlan(
        lessonId: String,
        day: Int,
        planJson: String,
        selections: Map<String, String>,
        planStorage: PlanStorage,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = uid() ?: throw IllegalStateException("Not signed in")
            val planObj = JSONObject(planJson)
            val planMap = jsonObjectToMap(planObj)
            val docId = planDocId(lessonId, day)
            val meta = planStorage.getDayMeta(lessonId, day)
            db.collection("users").document(userId)
                .collection("plans")
                .document(docId)
                .set(
                    buildPlanDocument(lessonId, day, planMap, selections, meta),
                    SetOptions.merge(),
                )
                .await()
            Unit
        }
    }

    suspend fun pushPlanMeta(
        lessonId: String,
        day: Int,
        planStorage: PlanStorage,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = uid() ?: throw IllegalStateException("Not signed in")
            val meta = planStorage.getDayMeta(lessonId, day)
            val docId = planDocId(lessonId, day)
            val payload = mutableMapOf<String, Any?>(
                "lessonId" to lessonId,
                "day" to day,
                "status" to meta.status.key,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            meta.feedback?.let { payload["feedback"] = it.key }
            if (meta.savedAt > 0L) payload["savedAt"] = meta.savedAt
            if (meta.completedAt > 0L) payload["completedAt"] = meta.completedAt
            db.collection("users").document(userId)
                .collection("plans")
                .document(docId)
                .set(payload, SetOptions.merge())
                .await()
            Unit
        }
    }

    suspend fun pullPlan(lessonId: String, day: Int, planStorage: PlanStorage): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val userId = uid() ?: throw IllegalStateException("Not signed in")
                val snap = db.collection("users").document(userId)
                    .collection("plans")
                    .document(planDocId(lessonId, day))
                    .get()
                    .await()
                if (!snap.exists()) return@runCatching false
                applyPlanDocument(snap.data ?: return@runCatching false, planStorage)
                true
            }
        }

    /** Pull cloud plans, then push any local plans that are newer or missing in cloud. */
    suspend fun syncAllPlans(planStorage: PlanStorage): Result<PlanSyncResult> = withContext(Dispatchers.IO) {
        runCatching {
            val userId = uid() ?: throw IllegalStateException("Not signed in")
            val plansRef = db.collection("users").document(userId).collection("plans")
            val snap = plansRef.get().await()
            val cloudUpdated = mutableMapOf<String, Long>()
            var pulled = 0
            for (doc in snap.documents) {
                val lessonId = doc.getString("lessonId") ?: continue
                val day = doc.getLong("day")?.toInt() ?: continue
                val cloudTs = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                cloudUpdated[planDocId(lessonId, day)] = cloudTs
                val localTs = planStorage.getPlanUpdatedAt(lessonId, day)
                if (cloudTs >= localTs) {
                    applyPlanDocument(doc.data ?: continue, planStorage)
                    pulled++
                }
            }
            var pushed = 0
            for (ref in planStorage.listLocalPlans()) {
                val docId = planDocId(ref.lessonId, ref.day)
                val localTs = planStorage.getPlanUpdatedAt(ref.lessonId, ref.day)
                val cloudTs = cloudUpdated[docId] ?: 0L
                if (localTs > cloudTs) {
                    val plan = planStorage.getPlan(ref.lessonId, ref.day) ?: continue
                    pushPlan(
                        ref.lessonId,
                        ref.day,
                        plan.toString(),
                        planStorage.getPlanSelections(ref.lessonId, ref.day),
                        planStorage,
                    ).getOrThrow()
                    pushed++
                }
            }
            PlanSyncResult(pulled = pulled, pushed = pushed, totalCloud = snap.size())
        }
    }

    suspend fun loadFlashcardImageUrls(lessonId: String): Map<String, String> = withContext(Dispatchers.IO) {
        runCatching {
            val snap = db.collection("catalog").document("flashcards")
                .collection("lessons")
                .document(safeLessonDocId(lessonId))
                .get()
                .await()
            if (!snap.exists()) return@runCatching emptyMap()
            val cards = snap.get("cards") as? List<*> ?: return@runCatching emptyMap()
            cards.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                val word = map["word"] as? String ?: return@mapNotNull null
                val url = map["imageUrl"] as? String ?: return@mapNotNull null
                if (url.isBlank()) null else word to url
            }.toMap()
        }.getOrDefault(emptyMap())
    }

    suspend fun loadTlmImageUrls(): Map<String, String> = withContext(Dispatchers.IO) {
        runCatching {
            val snap = db.collection("catalog").document("tlmResources").get().await()
            if (!snap.exists()) return@runCatching emptyMap()
            val resources = snap.get("resources") as? List<*> ?: return@runCatching emptyMap()
            resources.mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                val id = map["id"] as? String ?: return@mapNotNull null
                val url = map["imageUrl"] as? String ?: return@mapNotNull null
                if (url.isBlank()) null else id to url
            }.toMap()
        }.getOrDefault(emptyMap())
    }

    private fun planDocId(lessonId: String, day: Int) = "${safeLessonDocId(lessonId)}_day$day"

    private fun safeLessonDocId(lessonId: String) = lessonId.replace(".", "_")

    @Suppress("UNCHECKED_CAST")
    private fun applyPlanDocument(data: Map<String, Any?>, planStorage: PlanStorage) {
        val lessonId = data["lessonId"] as? String ?: return
        val day = (data["day"] as? Number)?.toInt() ?: return
        val planData = data["plan"] as? Map<*, *>
        val selections = selectionsToMap(data["selections"] as? Map<*, *>)
        val updatedAt = (data["updatedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time
            ?: System.currentTimeMillis()
        if (planData != null) {
            val json = mapToJsonObject(planData).toString()
            planStorage.savePlan(lessonId, day, json, selections, updatedAt)
        }
        val status = if (data["status"] != null) {
            DayPlanStatus.fromKey(data["status"] as? String)
        } else if (planData != null) {
            DayPlanStatus.PLANNED
        } else {
            DayPlanStatus.NOT_STARTED
        }
        val feedback = PlanFeedback.fromKey(data["feedback"] as? String)
        val savedAt = (data["savedAt"] as? Number)?.toLong() ?: 0L
        val completedAt = (data["completedAt"] as? Number)?.toLong() ?: 0L
        if (data["status"] != null || data["feedback"] != null || completedAt > 0L || savedAt > 0L) {
            planStorage.applyCloudMeta(lessonId, day, status, feedback, savedAt, completedAt)
        }
    }

    private fun buildPlanDocument(
        lessonId: String,
        day: Int,
        planMap: Map<String, Any?>,
        selections: Map<String, String>,
        meta: DayPlanMeta,
    ): Map<String, Any?> {
        val doc = mutableMapOf<String, Any?>(
            "lessonId" to lessonId,
            "day" to day,
            "plan" to planMap,
            "selections" to selections,
            "teacherResources" to selections["tlms"].orEmpty(),
            "status" to meta.status.key,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        meta.feedback?.let { doc["feedback"] = it.key }
        if (meta.savedAt > 0L) doc["savedAt"] = meta.savedAt
        if (meta.completedAt > 0L) doc["completedAt"] = meta.completedAt
        return doc
    }

    private fun selectionsToMap(raw: Map<*, *>?): Map<String, String> {
        if (raw == null) return emptyMap()
        return raw.mapNotNull { (k, v) ->
            val key = k as? String ?: return@mapNotNull null
            key to v.toString()
        }.toMap()
    }

    private fun jsonObjectToMap(obj: JSONObject): Map<String, Any?> {
        val out = mutableMapOf<String, Any?>()
        obj.keys().forEach { key ->
            out[key] = jsonValueToFirestore(obj.get(key))
        }
        return out
    }

    private fun jsonValueToFirestore(value: Any?): Any? = when (value) {
        null -> null
        is JSONObject -> jsonObjectToMap(value)
        is org.json.JSONArray -> (0 until value.length()).map { jsonValueToFirestore(value.get(it)) }
        else -> value
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapToJsonObject(map: Map<*, *>): JSONObject {
        val out = JSONObject()
        map.forEach { (k, v) ->
            val key = k as? String ?: return@forEach
            out.put(key, firestoreValueToJson(v))
        }
        return out
    }

    private fun firestoreValueToJson(value: Any?): Any? = when (value) {
        null -> JSONObject.NULL
        is Map<*, *> -> mapToJsonObject(value)
        is List<*> -> org.json.JSONArray(value.map { firestoreValueToJson(it) })
        else -> value
    }
}

data class PlanSyncResult(val pulled: Int, val pushed: Int, val totalCloud: Int)

private fun UserPreferences.toFirestoreMap(): Map<String, Any?> {
    val currentLessons = mutableMapOf<String, String>()
    getTeacherGrades().forEach { grade ->
        getTeacherSubjects().forEach { subject ->
            val id = getCurrentLesson(grade, subject)
            if (id.isNotBlank()) currentLessons["${grade}_$subject"] = id
        }
    }
    return mapOf(
        "teacherName" to teacherName,
        "phoneNumber" to phoneNumber,
        "language" to language,
        "state" to state,
        "district" to district,
        "adminType" to adminType,
        "zpName" to zpName,
        "corpName" to corpName,
        "medium" to medium,
        "englishComfort" to englishComfort,
        "schoolName" to schoolName,
        "location" to location,
        "pinCode" to pinCode,
        "studentCount" to studentCount,
        "internetAccess" to internetAccess,
        "printingAccess" to printingAccess,
        "teacherGrades" to getTeacherGrades(),
        "teacherSubjects" to getTeacherSubjects(),
        "teacherResources" to getTeacherResources(),
        "currentLessons" to currentLessons,
        "profileComplete" to profileComplete,
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Suppress("UNCHECKED_CAST")
fun UserPreferences.applyFromFirestore(data: Map<String, Any?>) {
    (data["teacherName"] as? String)?.let { teacherName = it }
    (data["phoneNumber"] as? String)?.let { phoneNumber = it }
    (data["language"] as? String)?.let { language = it }
    (data["state"] as? String)?.let { state = it }
    (data["district"] as? String)?.let { district = it }
    (data["adminType"] as? String)?.let { adminType = it }
    (data["zpName"] as? String)?.let { zpName = it }
    (data["corpName"] as? String)?.let { corpName = it }
    (data["medium"] as? String)?.let { medium = it }
    (data["englishComfort"] as? String)?.let { englishComfort = it }
    (data["schoolName"] as? String)?.let { schoolName = it }
    (data["location"] as? String)?.let { location = it }
    (data["pinCode"] as? String)?.let { pinCode = it }
    (data["studentCount"] as? Number)?.let { studentCount = it.toInt() }
    (data["internetAccess"] as? String)?.let { internetAccess = it }
    (data["printingAccess"] as? String)?.let { printingAccess = it }
    (data["teacherGrades"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }?.let { setTeacherGrades(it) }
    (data["teacherSubjects"] as? List<*>)?.mapNotNull { it as? String }?.let { setTeacherSubjects(it) }
    (data["teacherResources"] as? List<*>)?.mapNotNull { it as? String }?.let { setTeacherResources(it) }
    (data["profileComplete"] as? Boolean)?.let { profileComplete = it }
    (data["tier"] as? String)?.let { cachedTier = it }
    (data["currentLessons"] as? Map<*, *>)?.forEach { (k, v) ->
        val key = k as? String ?: return@forEach
        val lessonId = v as? String ?: return@forEach
        val parts = key.split("_")
        if (parts.size >= 2) {
            val grade = parts[0].toIntOrNull() ?: return@forEach
            val subject = parts.drop(1).joinToString("_")
            setCurrentLesson(grade, subject, lessonId)
        }
    }
}
