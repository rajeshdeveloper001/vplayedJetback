package com.vplayed.core.geo.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.vplayed.core.geo.GeoRefreshOutcome
import com.vplayed.core.geo.GeoRestrictionRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/** Requests a geo refresh. Also the explicit-refresh entry point for the rest of the app. */
interface GeoRefreshScheduler {
    fun requestRefresh()
}

/**
 * Runs [GeoRefreshWorker] as unique work: a new request replaces (and cancels) a pending or
 * running one, so connectivity events never stack up duplicate refreshes, and a request made for
 * a network that has since been replaced is dropped.
 */
@Singleton
class WorkManagerGeoRefreshScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : GeoRefreshScheduler {

    override fun requestRefresh() {
        val request = OneTimeWorkRequestBuilder<GeoRefreshWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    private companion object {
        const val WORK_NAME = "geo-restriction-refresh"
        const val BACKOFF_SECONDS = 10L
    }
}

/** What the worker tells WorkManager after one attempt. */
enum class GeoWorkDecision { SUCCESS, RETRY, FAILURE }

/** Retries transient failures up to [GeoRefreshWorker.MAX_ATTEMPTS] attempts in total. */
fun GeoRefreshOutcome.toWorkDecision(runAttemptCount: Int): GeoWorkDecision = when (this) {
    GeoRefreshOutcome.Success, GeoRefreshOutcome.InvalidResponse -> GeoWorkDecision.SUCCESS
    GeoRefreshOutcome.TransientFailure ->
        if (runAttemptCount + 1 < GeoRefreshWorker.MAX_ATTEMPTS) GeoWorkDecision.RETRY else GeoWorkDecision.FAILURE
}

/**
 * One geo refresh attempt. It is not a network listener: [GeoRestrictionSync][com.vplayed.core.geo.GeoRestrictionSync]
 * enqueues it when connectivity changes. Dependencies come from a Hilt entry point, so the default
 * WorkManager initializer works unchanged.
 */
class GeoRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun geoRestrictionRepository(): GeoRestrictionRepository
    }

    override suspend fun doWork(): Result {
        val decision = try {
            val repository = EntryPointAccessors
                .fromApplication(applicationContext, Dependencies::class.java)
                .geoRestrictionRepository()
            repository.refresh().toWorkDecision(runAttemptCount)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // Dependency graph unavailable or an unexpected bug: never crash the app for this.
            GeoWorkDecision.FAILURE
        }
        return when (decision) {
            GeoWorkDecision.SUCCESS -> Result.success()
            GeoWorkDecision.RETRY -> Result.retry()
            GeoWorkDecision.FAILURE -> Result.failure()
        }
    }

    companion object {
        const val MAX_ATTEMPTS = 3
    }
}
