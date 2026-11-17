package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.random.Random

@Keep
@IgnoreExtraProperties
data class SubscriberContract(
    @DocumentId val id: String = "",
    val userId: String = "",
    val contractNumber: String = "",
    val fullName: String = "",
    val documentIdNumber: String = "",
    val contactEmail: String = "",
    val virtualNumber: String = "",
    val tariffId: String = TariffPlan.FREE_0.id,
    val tariffPriceRub: Long = 0L,
    val status: String = CONTRACT_STATUS_ACTIVE,
    val signatureStrokeCount: Long = 0L,
    val signaturePointsJson: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun activeContract(): Boolean = status == CONTRACT_STATUS_ACTIVE

    fun tariffPlan(): TariffPlan = TariffPlan.fromId(tariffId)

    companion object {
        const val PRIMARY_DOC_ID = "primary"
        const val CONTRACT_STATUS_ACTIVE = "ACTIVE"
        const val CONTRACT_STATUS_TERMINATED = "TERMINATED"
    }
}

@Keep
@IgnoreExtraProperties
data class CallRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val callerVirtualNumber: String = "",
    val recipientNumber: String = "",
    val recipientName: String = "",
    val networkType: String = "Wi-Fi",
    val tariffId: String = TariffPlan.FREE_0.id,
    val callMode: String = CALL_MODE_INTERNET_TO_REAL,
    val status: String = CALL_STATUS_INITIATED,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    companion object {
        const val CALL_MODE_INTERNET_TO_REAL = "INTERNET_TO_REAL"
        const val CALL_STATUS_INITIATED = "INITIATED"
    }
}

enum class TariffPlan(
    val id: String,
    val title: String,
    val priceRub: Long,
    val badge: String,
    val minutesSummary: String,
    val features: List<String>
) {
    FREE_0(
        id = "free_0",
        title = "К mobile Старт",
        priceRub = 0L,
        badge = "Бесплатный • 0 ₽",
        minutesSummary = "30 мин по РФ • Безлимит внутри К mobile",
        features = listOf(
            "Виртуальный номер с префиксом +7 990",
            "Первый бесплатный тариф — 0 рублей навсегда",
            "Звонки через интернет-сеть с выходом в реальный вызов",
            "30 минут в месяц на номера других операторов",
            "Подключение по абонентскому договору К mobile"
        )
    ),
    PRO_100(
        id = "pro_100",
        title = "К mobile 100",
        priceRub = 100L,
        badge = "Хит • 100 ₽",
        minutesSummary = "500 мин по РФ • Приоритетный HD-канал",
        features = listOf(
            "Виртуальный номер +7 990 с выбором красивой комбинации",
            "Абонентская плата 100 рублей в месяц",
            "500 минут исходящих звонков на любые номера РФ",
            "Приоритетная маршрутизация интернет → реальный звонок",
            "HD-качество голоса и безлимит внутри сети К mobile"
        )
    );

    companion object {
        fun fromId(raw: String): TariffPlan =
            entries.firstOrNull { it.id == raw } ?: FREE_0
    }
}

data class DeviceContact(
    val name: String,
    val phoneNumber: String
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

fun generateVirtualNumbers7990(count: Int = 6): List<String> {
    val curated = listOf(
        "+79901007700",
        "+79902405500",
        "+79903308899",
        "+79905051010"
    )
    val generated = buildList {
        addAll(curated.shuffled().take(2))
        while (size < count) {
            val mid = Random.nextInt(100, 999)
            val tail = Random.nextInt(1000, 9999)
            val candidate = "+7990$mid$tail"
            if (candidate !in this) add(candidate)
        }
    }
    return generated
}

fun generateContractNumber(): String {
    val suffix = Random.nextInt(1000, 9999)
    return "KM-2026-$suffix"
}

fun formatRussianPhone(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return if (digits.length == 11 && (digits.startsWith("7") || digits.startsWith("8"))) {
        val code = digits.substring(1, 4)
        val p1 = digits.substring(4, 7)
        val p2 = digits.substring(7, 9)
        val p3 = digits.substring(9, 11)
        "+7 ($code) $p1-$p2-$p3"
    } else {
        raw
    }
}

fun normalizeDialablePhone(raw: String): String {
    val trimmed = raw.trim()
    val hasPlus = trimmed.startsWith("+")
    val digits = trimmed.filter { it.isDigit() }
    if (digits.isEmpty()) return ""
    return when {
        digits.length == 11 && digits.startsWith("8") -> "+7" + digits.drop(1)
        digits.length == 11 && digits.startsWith("7") -> "+$digits"
        digits.length == 10 -> "+7$digits"
        hasPlus -> "+$digits"
        else -> digits
    }
}
