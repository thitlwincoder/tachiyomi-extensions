package eu.kanade.tachiyomi.extension.all.shwetoon

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject

open class ShweToon : HttpSource() {

    override val name = "ShweToon"
    override val lang = "my"
    override val baseUrl = "https://cdn.shwetoon.com"
    private val cdn = "https://cdn.shwetoon.com"
    override val supportsLatest = true

    override fun headersBuilder() = super.headersBuilder()
        .add("User-Agent", USER_AGENT)
        .add("Accept", "application/json")

    // ----- Listing (popular / latest) : all series from collections.json -----

    override fun popularMangaRequest(page: Int) = GET("$cdn/collections/collections.json", headers)
    override fun popularMangaParse(response: Response): MangasPage = parseSeriesList(response)

    override fun latestUpdatesRequest(page: Int) = popularMangaRequest(page)
    override fun latestUpdatesParse(response: Response) = parseSeriesList(response)

    private fun parseSeriesList(response: Response): MangasPage {
        val arr = JSONArray(response.body!!.string())
        val list = mutableListOf<SManga>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(SManga.create().apply {
                url = o.getString("id")
                title = pickTitle(o)
                thumbnail_url = null
            })
        }
        return MangasPage(list, false)
    }

    // ----- Search (filter collections client-side) -----

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val q = java.net.URLEncoder.encode(query, "UTF-8")
        return GET("$cdn/collections/collections.json?q=$q", headers)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val arr = JSONArray(response.body!!.string())
        val q = response.request.url.queryParameter("q").orEmpty().lowercase()
        val list = mutableListOf<SManga>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val title = pickTitle(o)
            val en = o.optString("title").lowercase()
            val mm = o.optString("titleMM").lowercase()
            if (q.isBlank() || title.lowercase().contains(q) || en.contains(q) || mm.contains(q)) {
                list.add(SManga.create().apply {
                    url = o.getString("id")
                    this.title = title
                })
            }
        }
        return MangasPage(list, false)
    }

    // ----- Manga details + chapters from collections/<id>.json -----

    override fun mangaDetailsRequest(manga: SManga) = GET("$cdn/collections/${manga.url}.json", headers)

    override fun mangaDetailsParse(response: Response): SManga {
        val arr = JSONArray(response.body!!.string())
        val manga = SManga.create()
        if (arr.length() == 0) return manga
        val o = arr.getJSONObject(0)
        manga.title = pickTitle(o)
        manga.thumbnail_url = o.optString("coverImageUrl").ifEmpty { null }
        manga.author = o.optString("authors").ifEmpty { o.optString("artists") }.ifEmpty { null }
        manga.artist = o.optString("artists").ifEmpty { null }
        manga.genre = o.optString("genres").ifEmpty { null }
        manga.status = when (o.optString("status", "").lowercase()) {
            "completed" -> SManga.COMPLETED
            "ongoing" -> SManga.ONGOING
            else -> SManga.UNKNOWN
        }
        val mm = o.optString("titleMM").ifEmpty { null }
        manga.description = mm?.let { "မြန်မာ: $it" } ?: o.optString("title").ifEmpty { null }
        return manga
    }

    override fun chapterListRequest(manga: SManga) = mangaDetailsRequest(manga)

    override fun chapterListParse(response: Response): List<SChapter> {
        val arr = JSONArray(response.body!!.string())
        val collectionId = response.request.url.toString()
            .substringAfter("/collections/")
            .substringBefore(".json")
        val list = mutableListOf<SChapter>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val comicId = o.getString("id")
            list.add(SChapter.create().apply {
                url = "$collectionId/$comicId"
                name = o.optString("titleMM").ifEmpty { o.optString("title") }
                chapter_number = (i + 1).toFloat()
            })
        }
        return list.reversed()
    }

    // ----- Pages : comic contents.json -> per sub-chapter image list -----

    override fun pageListRequest(chapter: SChapter) = GET("$cdn/${chapter.url}/contents.json", headers)

    override fun pageListParse(response: Response): List<Page> {
        val comic = JSONObject(response.body!!.string())
        val subChapters = comic.optJSONArray("chapters") ?: JSONArray()
        val base = response.request.url.toString().removeSuffix("/contents.json")
        val pages = mutableListOf<Page>()
        var index = 0
        for (j in 0 until subChapters.length()) {
            val subId = subChapters.getJSONObject(j).getString("id")
            val leafResp = client.newCall(GET("$base/$subId/contents.json", headers)).execute()
            val imgs = JSONArray(leafResp.body!!.string())
            for (k in 0 until imgs.length()) {
                pages.add(Page(index++, "", imgs.getString(k)))
            }
        }
        return pages
    }

    override fun imageRequest(page: Page): Request {
        return GET(page.imageUrl!!, headersBuilder().add("Referer", "https://shwetoon.com").build())
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()

    override fun getFilterList() = FilterList()

    private fun pickTitle(o: JSONObject): String {
        val mm = o.optString("titleMM").ifEmpty { null }
        return mm ?: o.optString("title")
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
