package com.tasomaniac.openwith.util

import android.app.Activity
import android.app.ActivityManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.LOLLIPOP_MR1
import android.text.format.DateUtils
import androidx.annotation.RequiresApi
import androidx.core.app.ShareCompat
import androidx.core.content.getSystemService
import com.tasomaniac.openwith.BuildConfig

sealed class CallerPackageExtractor {

    abstract fun extract(): String?

    companion object {

        @JvmStatic
        fun from(activity: Activity): CallerPackageExtractor {
            val callerPackage = ShareCompat.getCallingPackage(activity)

            return when {
                callerPackage != null -> SimpleExtractor(callerPackage)
                SDK_INT < LOLLIPOP -> LegacyExtractor(activity)
                SDK_INT >= LOLLIPOP_MR1 -> LollipopExtractor(activity)
                else -> SimpleExtractor(null)
            }

        }
    }
}

private class SimpleExtractor(private val callerPackage: String?) : CallerPackageExtractor() {

    override fun extract(): String? {
        return callerPackage
    }
}

@Suppress("DEPRECATION")
private class LegacyExtractor(context: Context) : CallerPackageExtractor() {

    private val activityManager = context.getSystemService<ActivityManager>()!!

    override fun extract(): String? {
        val runningTasks = activityManager.getRunningTasks(1)
        return runningTasks[0].baseActivity.packageName
    }
}

@RequiresApi(LOLLIPOP_MR1)
private class LollipopExtractor(context: Context) : CallerPackageExtractor() {

    private val usageStatsManager = context.getSystemService<UsageStatsManager>()

    override fun extract() = usageStatsManager?.recentUsageStats()?.mostRecentPackage()

    /**
     * Returns recently used apps for the last 10 seconds
     */
    private fun UsageStatsManager.recentUsageStats(): List<UsageStats>? {
        return try {
            val time = System.currentTimeMillis()
            queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                time - 10 * DateUtils.SECOND_IN_MILLIS,
                time
            )
        } catch (ignored: Exception) {
            null
        }
    }

    private fun List<UsageStats>.mostRecentPackage() =
        filter {
            it.packageName !in listOf(BuildConfig.APPLICATION_ID, "android")
        }.maxBy {
            it.lastTimeUsed
        }?.packageName
}
