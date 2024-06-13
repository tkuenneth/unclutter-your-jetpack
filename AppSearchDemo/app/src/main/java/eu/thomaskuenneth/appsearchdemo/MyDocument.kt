package eu.thomaskuenneth.appsearchdemo

import androidx.appsearch.annotation.Document
import androidx.appsearch.app.AppSearchSchema


@Document
data class MyDocument(
    @Document.Namespace
    val namespace: String,

    @Document.Id
    val id: String,

    @Document.Score
    val score: Int,

    @Document.StringProperty(indexingType = AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
    val message: String
)
