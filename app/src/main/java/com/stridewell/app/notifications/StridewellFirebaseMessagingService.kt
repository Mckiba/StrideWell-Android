package com.stridewell.app.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.stridewell.app.data.NotificationsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StridewellFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationsRepository: NotificationsRepository

    /**
     * Called when FCM assigns a new registration token (first launch, token refresh, or
     * after uninstall/reinstall). Registers it with the backend immediately.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            notificationsRepository.registerToken(token)
        }
    }

    /**
     * Called when a data or notification message is received while the app is in foreground.
     * Extracts title/body/deep_link and displays a local notification.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title    = message.notification?.title    ?: message.data["title"]    ?: return
        val body     = message.notification?.body     ?: message.data["body"]     ?: return
        val deepLink = message.data["deep_link"] ?: "home"

        NotificationHelper.createChannel(this)
        NotificationHelper.showNotification(
            context    = this,
            title      = title,
            body       = body,
            deepLink   = deepLink,
        )
    }
}
