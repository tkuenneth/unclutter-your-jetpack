package eu.thomaskuenneth.appsearchdemo

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.localstorage.LocalStorage
import androidx.appsearch.platformstorage.PlatformStorage
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

private const val DATABASE_NAME = "appsearchdemo"

class AppSearchObserver(private val context: Context) : DefaultLifecycleObserver {

    lateinit var sessionFuture: ListenableFuture<AppSearchSession>

    override fun onResume(owner: LifecycleOwner) {
        sessionFuture = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PlatformStorage.createSearchSession(
                PlatformStorage.SearchContext.Builder(context, DATABASE_NAME).build()
            )
        } else {
            LocalStorage.createSearchSession(
                LocalStorage.SearchContext.Builder(context, DATABASE_NAME).build()
            )
        }
    }

    @SuppressLint("CheckResult")
    override fun onPause(owner: LifecycleOwner) {
        Futures.transform<AppSearchSession, Unit>(
            sessionFuture, { session ->
                session?.close()
                Unit
            }, context.mainExecutor
        )
    }
}
