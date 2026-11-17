package com.example.system

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.model.DeviceContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

data class NetworkStatus(
    val isConnected: Boolean,
    val transportLabel: String
)

data class SystemPermissionItem(
    val permission: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val isEssentialForCalling: Boolean
)

object SystemBridgeHelper {

    fun requiredRuntimePermissions(): List<String> = buildList {
        add(Manifest.permission.CALL_PHONE)
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.READ_PHONE_STATE)
        add(Manifest.permission.READ_CONTACTS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun checkPermissionsState(context: Context): List<SystemPermissionItem> {
        fun granted(perm: String): Boolean =
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

        return buildList {
            add(
                SystemPermissionItem(
                    permission = Manifest.permission.CALL_PHONE,
                    title = "Телефонные звонки (CALL_PHONE)",
                    description = "Позволяет переводить интернет-сессию К mobile в реальный телефонный вызов.",
                    isGranted = granted(Manifest.permission.CALL_PHONE),
                    isEssentialForCalling = true
                )
            )
            add(
                SystemPermissionItem(
                    permission = Manifest.permission.RECORD_AUDIO,
                    title = "Микрофон (RECORD_AUDIO)",
                    description = "Передача вашего голоса при соединении через интернет-сеть К mobile.",
                    isGranted = granted(Manifest.permission.RECORD_AUDIO),
                    isEssentialForCalling = true
                )
            )
            add(
                SystemPermissionItem(
                    permission = Manifest.permission.READ_PHONE_STATE,
                    title = "Состояние связи (READ_PHONE_STATE)",
                    description = "Отслеживание статуса линии и корректная маршрутизация вызова Android.",
                    isGranted = granted(Manifest.permission.READ_PHONE_STATE),
                    isEssentialForCalling = true
                )
            )
            add(
                SystemPermissionItem(
                    permission = Manifest.permission.READ_CONTACTS,
                    title = "Контакты (READ_CONTACTS)",
                    description = "Выбор абонентов из телефонной книги для быстрого набора.",
                    isGranted = granted(Manifest.permission.READ_CONTACTS),
                    isEssentialForCalling = false
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(
                    SystemPermissionItem(
                        permission = Manifest.permission.POST_NOTIFICATIONS,
                        title = "Уведомления (POST_NOTIFICATIONS)",
                        description = "Статус активного интернет-соединения и договора К mobile.",
                        isGranted = granted(Manifest.permission.POST_NOTIFICATIONS),
                        isEssentialForCalling = false
                    )
                )
            }
        }
    }

    fun hasCallPhonePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED

    fun hasAudioPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    fun hasContactsPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    fun currentNetworkStatus(context: Context): NetworkStatus {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkStatus(false, "Нет сети")
        val activeNetwork = cm.activeNetwork ?: return NetworkStatus(false, "Нет сети")
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkStatus(false, "Нет сети")
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (!hasInternet) return NetworkStatus(false, "Нет сети")

        val label = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "LTE/5G"
            else -> "Internet"
        }
        return NetworkStatus(true, label)
    }

    fun observeNetworkStatus(context: Context): Flow<NetworkStatus> = callbackFlow {
        val appContext = context.applicationContext
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        trySend(currentNetworkStatus(appContext))

        if (cm == null) {
            awaitClose {}
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(currentNetworkStatus(appContext))
            }

            override fun onLost(network: Network) {
                trySend(currentNetworkStatus(appContext))
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(currentNetworkStatus(appContext))
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        runCatching { cm.registerNetworkCallback(request, callback) }
        awaitClose {
            runCatching { cm.unregisterNetworkCallback(callback) }
        }
    }

    suspend fun loadDeviceContacts(context: Context): List<DeviceContact> =
        withContext(Dispatchers.IO) {
            if (!hasContactsPermission(context)) return@withContext emptyList()
            val results = mutableListOf<DeviceContact>()
            val seen = HashSet<String>()
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            runCatching {
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
                )?.use { cursor ->
                    val nameIdx =
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx =
                        cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (cursor.moveToNext() && results.size < 100) {
                        val name = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty() else ""
                        val number = if (numIdx >= 0) cursor.getString(numIdx).orEmpty() else ""
                        val clean = number.replace(Regex("[\\s\\-()]"), "")
                        if (clean.isNotBlank() && seen.add(clean)) {
                            results.add(
                                DeviceContact(
                                    name = name.ifBlank { number },
                                    phoneNumber = number
                                )
                            )
                        }
                    }
                }
            }
            results
        }

    /**
     * Initiates a real Android phone call!
     * Uses ACTION_CALL when CALL_PHONE permission is granted so the user enters a real active call;
     * safely falls back to ACTION_DIAL if CALL_PHONE is not yet granted.
     */
    fun launchRealAndroidCall(context: Context, phoneNumber: String): Boolean {
        val telUri = Uri.fromParts("tel", phoneNumber, null)
        return try {
            val action = if (hasCallPhonePermission(context)) {
                Intent.ACTION_CALL
            } else {
                Intent.ACTION_DIAL
            }
            val intent = Intent(action, telUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: SecurityException) {
            runCatching {
                val fallback = Intent(Intent.ACTION_DIAL, telUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
                true
            }.getOrDefault(false)
        } catch (_: Exception) {
            false
        }
    }

    fun openAppPermissionsSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
