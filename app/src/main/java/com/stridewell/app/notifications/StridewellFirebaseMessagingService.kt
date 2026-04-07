package com.stridewell.app.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.stridewell.app.data.ActivityRepository
import com.stridewell.app.data.NotificationsRepository
import com.stridewell.app.data.PlanRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StridewellFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationsRepository: NotificationsRepository

    @Inject
    lateinit var activityRepository: ActivityRepository

    @Inject
    lateinit var planRepository: PlanRepository

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

        val deepLink = message.data["deep_link"] ?: "home"
        val runId = message.data["run_id"]
        val planVersionId = message.data["plan_version_id"]
        val title = message.notification?.title ?: message.data["title"]
        val body = message.notification?.body ?: message.data["body"] ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            if (deepLink == "home" && !runId.isNullOrBlank()) {
                activityRepository.setLastSyncedRun(runId, body)
            }
            if (deepLink == "plan_change" && !planVersionId.isNullOrBlank()) {
                planRepository.setCurrentPlanVersionId(planVersionId)
            }
        }

        if (title.isNullOrBlank()) return

        NotificationHelper.createChannel(this)
        NotificationHelper.showNotification(
            context    = this,
            title      = title,
            body       = body,
            deepLink   = deepLink,
            runId      = runId,
            planVersionId = planVersionId,
        )
    }
}
