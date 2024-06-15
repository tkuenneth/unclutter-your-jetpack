package eu.thomaskuenneth.appsearchdemo

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appsearch.app.AppSearchBatchResult
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.app.GenericDocument
import androidx.appsearch.app.PutDocumentsRequest
import androidx.appsearch.app.SearchResult
import androidx.appsearch.app.SearchResults
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.app.SetSchemaRequest
import androidx.appsearch.exceptions.AppSearchException
import androidx.appsearch.localstorage.LocalStorage
import androidx.appsearch.platformstorage.PlatformStorage
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

private const val TAG = "AppSearchDemoActivity"
private const val DATABASE_NAME = "appsearchdemo"

class AppSearchObserver(private val context: Context) : DefaultLifecycleObserver {

    lateinit var sessionFuture: ListenableFuture<AppSearchSession>

    override fun onResume(owner: LifecycleOwner) {
        sessionFuture = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PlatformStorage.createSearchSession(
                PlatformStorage.SearchContext.Builder(context, DATABASE_NAME)
                    .build()
            )
        } else {
            LocalStorage.createSearchSession(
                LocalStorage.SearchContext.Builder(context, DATABASE_NAME)
                    .build()
            )
        }
    }

    @SuppressLint("CheckResult")
    override fun onPause(owner: LifecycleOwner) {
        Futures.transform<AppSearchSession, Unit>(
            sessionFuture,
            { session ->
                session?.close()
                Unit
            }, context.mainExecutor
        )
    }
}

class AppSearchDemoActivity : AppCompatActivity() {

    private lateinit var appSearchObserver: AppSearchObserver

    private val message: MutableStateFlow<String> = MutableStateFlow("")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appSearchObserver = AppSearchObserver(applicationContext)
        lifecycle.addObserver(appSearchObserver)
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                setSchema()
                addDocument()
                search()
                persist()
            }
        }
        setContent {
            val text by message.collectAsState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun setSchema() {
        val setSchemaRequest =
            SetSchemaRequest.Builder().addDocumentClasses(MyDocument::class.java)
                .build()
        Futures.transformAsync(
            appSearchObserver.sessionFuture,
            { session ->
                session.setSchema(setSchemaRequest)
            }, mainExecutor
        )
    }

    private fun addDocument() {
        val doc = MyDocument(
            namespace = packageName,
            id = UUID.randomUUID().toString(),
            score = 10,
            message = "Hello, this doc was created ${Date()}"
        )
        val putRequest = PutDocumentsRequest.Builder().addDocuments(doc).build()
        val putFuture = Futures.transformAsync(
            appSearchObserver.sessionFuture,
            { session ->
                session.put(putRequest)
            }, mainExecutor
        )
        Futures.addCallback(
            putFuture,
            object : FutureCallback<AppSearchBatchResult<String, Void>?> {
                override fun onSuccess(result: AppSearchBatchResult<String, Void>?) {
                    output("successfulResults = ${result?.successes?.entries}")
                    output("failedResults = ${result?.failures}")
                }

                override fun onFailure(t: Throwable) {
                    output("Failed to put document(s).")
                    Log.e(TAG, "Failed to put document(s).", t)
                }
            },
            mainExecutor
        )
    }

    private fun search() {
        val searchSpec = SearchSpec.Builder()
            .addFilterNamespaces(packageName)
            .setResultCountPerPage(100)
            .build()
        val searchFuture = Futures.transform(
            appSearchObserver.sessionFuture,
            { session ->
                session?.search("hello", searchSpec)
            },
            mainExecutor
        )
        Futures.addCallback(
            searchFuture,
            object : FutureCallback<SearchResults?> {
                override fun onSuccess(searchResults: SearchResults?) {
                    searchResults?.let {
                        iterateSearchResults(searchResults)
                    }
                }

                override fun onFailure(t: Throwable) {
                    Log.e("TAG", "Failed to search in AppSearch.", t)
                }
            },
            mainExecutor
        )
    }

    @SuppressLint("CheckResult")
    private fun iterateSearchResults(searchResults: SearchResults) {
        Futures.transform(
            searchResults.nextPage,
            { page: List<SearchResult>? ->
                page?.forEach { current ->
                    val genericDocument: GenericDocument = current.genericDocument
                    val schemaType = genericDocument.schemaType
                    val document: MyDocument? = try {
                        if (schemaType == "MyDocument") {
                            genericDocument.toDocumentClass(MyDocument::class.java)
                        } else null
                    } catch (e: AppSearchException) {
                        Log.e(
                            TAG,
                            "Failed to convert GenericDocument to MyDocument",
                            e
                        )
                        null
                    }
                    output("Found ${document?.message}")
                }
            },
            mainExecutor
        )
    }

    private fun persist() {
        val requestFlushFuture = Futures.transformAsync(
            appSearchObserver.sessionFuture,
            { session -> session.requestFlush() }, mainExecutor
        )
        Futures.addCallback(requestFlushFuture, object : FutureCallback<Void?> {
            override fun onSuccess(result: Void?) {
                output("Success! Database updates have been persisted to disk")
            }

            override fun onFailure(t: Throwable) {
                Log.e(TAG, "Failed to flush database updates.", t)
            }
        }, mainExecutor)
    }

    private fun output(s: String) {
        message.update { "${if (it.isNotEmpty()) "$it\n" else ""}$s" }
    }
}
