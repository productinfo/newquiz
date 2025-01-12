package com.infinitepower.newquiz.data.worker.wordle

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.infinitepower.newquiz.core.notification.wordle.DailyWordleNotificationService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DailyWordleNotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val dailyWordleNotificationService: DailyWordleNotificationService
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val notificationManager = NotificationManagerCompat.from(applicationContext)
        if (!notificationManager.areNotificationsEnabled()) return Result.failure()

        dailyWordleNotificationService.showNotification()

        return Result.success()
    }
}