/*
 * Copyright © 2021 YUMEMI Inc. All rights reserved.
 */
package jp.co.yumemi.android.code_check

import android.content.Context
import android.os.Parcelable
import androidx.lifecycle.ViewModel
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import jp.co.yumemi.android.code_check.TopActivity.Companion.lastSearchDate
import kotlinx.coroutines.*
import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject
import java.util.*

// OneFragmentにリポジトリのデータを渡すview model
class OneViewModel(val context: Context) : ViewModel() {

    // githubのapiからデータを受け取って、入力に対する検索結果のリストを返す
    fun searchResults(inputText: String): List<Item> = runBlocking {
        val client = HttpClient(Android)

        return@runBlocking async {
            val response: HttpResponse = client.get("https://api.github.com/search/repositories") {
                header("Accept", "application/vnd.github.v3+json")
                parameter("q", inputText)
            }

            val items = mutableListOf<Item>()

            val jsonBody = runCatching {
                JSONObject(response.receive<String>())
            }.fold(
                onSuccess = { it },
                onFailure = { JSONObject() }
            )

            val jsonItems = jsonBody.optJSONArray("items") ?: return@async items.toList()

            // 検索結果をアイテムに格納
            for (i in 0 until jsonItems.length()) {
                jsonItems.optJSONObject(i)?.let {
                    val item = Item(
                        name = it.optString("full_name"),
                        // TODO: ownerIconのURLがnullだったら、デフォルトのownerIconURLを返すようにする
                        ownerIconUrl = it.optJSONObject("owner")!!.optString("avatar_url"),
                        language = context.getString(
                            R.string.written_language,
                            it.optString("language")
                        ),
                        stargazersCount = it.optLong("stargazers_count"),
                        watchersCount = it.optLong("watchers_count"),
                        forksCount = it.optLong("forks_count"),
                        openIssuesCount = it.optLong("open_issues_count")
                    )

                    items.add(item)
                }

            }

            lastSearchDate = Date()

            return@async items.toList()
        }.await()
    }
}

// 検索結果のデータ
@Parcelize
data class Item(
    val name: String,
    val ownerIconUrl: String,
    val language: String,
    val stargazersCount: Long,
    val watchersCount: Long,
    val forksCount: Long,
    val openIssuesCount: Long,
) : Parcelable