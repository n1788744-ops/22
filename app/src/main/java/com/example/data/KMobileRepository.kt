package com.example.data

import android.content.Context
import com.example.R
import com.example.model.CallRecord
import com.example.model.SubscriberContract
import com.example.model.TariffPlan
import com.example.model.generateContractNumber
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class KMobileRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireSignedInUid(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Требуется авторизация через Google.")
        check(uid == expectedUserId) { "Сменился аккаунт; запись отклонена." }
        return uid
    }

    fun observePrimaryContract(userId: String): Flow<SubscriberContract?> =
        db.collection("users")
            .document(userId)
            .collection("contracts")
            .document(SubscriberContract.PRIMARY_DOC_ID)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) {
                    null
                } else {
                    snapshot.toObject(
                        SubscriberContract::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
            }

    fun observeCallHistory(userId: String): Flow<List<CallRecord>> =
        db.collection("users")
            .document(userId)
            .collection("calls")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(
                    CallRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun signOrRenewContract(
        userId: String,
        fullName: String,
        documentIdNumber: String,
        contactEmail: String,
        virtualNumber: String,
        tariff: TariffPlan,
        signatureStrokeCount: Int,
        signaturePointsJson: String
    ): Result<String> {
        val docPath = "users/$userId/contracts/${SubscriberContract.PRIMARY_DOC_ID}"
        return try {
            val uid = requireSignedInUid(userId)
            val cleanName = fullName.trim().take(120)
            val cleanDoc = documentIdNumber.trim().take(40)
            val cleanEmail = contactEmail.trim().take(120)
            val cleanNumber = virtualNumber.trim()
            val cleanSignature = signaturePointsJson.trim().take(20000)

            require(cleanName.length >= 2) { "Введите корректное ФИО (от 2 символов)." }
            require(cleanDoc.length >= 4) { "Введите серию и номер документа (от 4 символов)." }
            require(cleanEmail.length >= 3) { "Введите контактный e-mail." }
            require(cleanNumber.matches(Regex("^\\+7990[0-9]{7}$"))) {
                "Виртуальный номер должен начинаться с +7990 и содержать 11 цифр."
            }
            require(signatureStrokeCount in 1..500 && cleanSignature.length >= 2) {
                "Поставьте подпись в поле договора."
            }

            val docRef = db.collection("users")
                .document(uid)
                .collection("contracts")
                .document(SubscriberContract.PRIMARY_DOC_ID)

            val contractNumber = generateContractNumber()
            val existing = docRef.get().await()

            if (!existing.exists()) {
                val createPayload = mapOf(
                    "userId" to uid,
                    "contractNumber" to contractNumber,
                    "fullName" to cleanName,
                    "documentIdNumber" to cleanDoc,
                    "contactEmail" to cleanEmail,
                    "virtualNumber" to cleanNumber,
                    "tariffId" to tariff.id,
                    "tariffPriceRub" to tariff.priceRub,
                    "status" to SubscriberContract.CONTRACT_STATUS_ACTIVE,
                    "signatureStrokeCount" to signatureStrokeCount.toLong(),
                    "signaturePointsJson" to cleanSignature,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            } else {
                val updatePayload = mapOf(
                    "contractNumber" to contractNumber,
                    "fullName" to cleanName,
                    "documentIdNumber" to cleanDoc,
                    "contactEmail" to cleanEmail,
                    "virtualNumber" to cleanNumber,
                    "tariffId" to tariff.id,
                    "tariffPriceRub" to tariff.priceRub,
                    "status" to SubscriberContract.CONTRACT_STATUS_ACTIVE,
                    "signatureStrokeCount" to signatureStrokeCount.toLong(),
                    "signaturePointsJson" to cleanSignature,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            }
            Result.success(contractNumber)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun switchTariff(userId: String, tariff: TariffPlan): Result<Unit> {
        val docPath = "users/$userId/contracts/${SubscriberContract.PRIMARY_DOC_ID}"
        return try {
            val uid = requireSignedInUid(userId)
            val docRef = db.collection("users")
                .document(uid)
                .collection("contracts")
                .document(SubscriberContract.PRIMARY_DOC_ID)
            docRef.update(
                mapOf(
                    "tariffId" to tariff.id,
                    "tariffPriceRub" to tariff.priceRub,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun terminateContract(userId: String): Result<Unit> {
        val docPath = "users/$userId/contracts/${SubscriberContract.PRIMARY_DOC_ID}"
        return try {
            val uid = requireSignedInUid(userId)
            val docRef = db.collection("users")
                .document(uid)
                .collection("contracts")
                .document(SubscriberContract.PRIMARY_DOC_ID)
            docRef.update(
                mapOf(
                    "status" to SubscriberContract.CONTRACT_STATUS_TERMINATED,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun logCall(
        userId: String,
        callerVirtualNumber: String,
        recipientNumber: String,
        recipientName: String,
        networkType: String,
        tariffId: String,
        callMode: String = CallRecord.CALL_MODE_INTERNET_TO_REAL,
        status: String = CallRecord.CALL_STATUS_INITIATED
    ): Result<Unit> {
        val colPath = "users/$userId/calls"
        return try {
            val uid = requireSignedInUid(userId)
            val cleanRecipient = recipientNumber.trim().take(32)
            require(cleanRecipient.length >= 3) { "Номер получателя слишком короткий." }
            require(callerVirtualNumber.matches(Regex("^\\+7990[0-9]{7}$"))) {
                "Некорректный виртуальный номер К mobile."
            }
            val safeNetwork = when (networkType) {
                "Wi-Fi", "LTE/5G", "Internet" -> networkType
                else -> "Internet"
            }
            val safeTariffId = if (tariffId == TariffPlan.PRO_100.id) {
                TariffPlan.PRO_100.id
            } else {
                TariffPlan.FREE_0.id
            }
            val docRef = db.collection("users").document(uid).collection("calls").document()
            val payload = mapOf(
                "userId" to uid,
                "callerVirtualNumber" to callerVirtualNumber,
                "recipientNumber" to cleanRecipient,
                "recipientName" to recipientName.trim().take(100),
                "networkType" to safeNetwork,
                "tariffId" to safeTariffId,
                "callMode" to callMode,
                "status" to status,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCallRecord(userId: String, callId: String): Result<Unit> {
        val docPath = "users/$userId/calls/$callId"
        return try {
            val uid = requireSignedInUid(userId)
            require(callId.isNotBlank()) { "Некорректный идентификатор записи." }
            db.collection("users")
                .document(uid)
                .collection("calls")
                .document(callId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
