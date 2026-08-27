package eu.kanade.tachiyomi.extension.all.mangamangomyanmar

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.nodes.Document

open class MangaMangaMyanmar : HttpSource() {

    override val name = "Manga Manga Myanmar"
    override val lang = "my"
    override val baseUrl = "https://mangamangomyanmar.com"
    override val supportsLatest = true

    override fun headersBuilder() = super.headersBuilder()
        .add("User-Agent", USER_AGENT)
        .add("Accept-Language", "en-US,en;q=0.9")

    // ----- Listing (popular / latest) -----

    override fun popularMangaRequest(page: Int): Request {
        val url = if (page == 1) "$baseUrl/manga/" else "$baseUrl/manga/page/$page/"
        return GET(url, headers)
    }

    override fun popularMangaParse(response: okhttp3.Response): MangasPage = parseMangaList(response.asJsoup())

    override fun latestUpdatesRequest(page: Int) = popularMangaRequest(page)
    override fun latestUpdatesParse(response: okhttp3.Response): MangasPage = parseMangaList(response.asJsoup())

    private fun parseMangaList(document: Document): MangasPage {
        val elements = document.select(
            ".page-item-detail.manga .post-title a, .c-tabs-item a[href*='/manga/']",
        )
        val mangas = elements.mapNotNull { a ->
            val href = a.absUrl("href")
            if (!href.contains("/manga/") || href.contains("/episode-")) return@mapNotNull null
            val title = a.text().trim().ifBlank { a.attr("title").trim() }.ifBlank { return@mapNotNull null }
            SManga.create().apply {
                url = href.removePrefix(baseUrl).removeSuffix("/") + "/"
                this.title = title
                thumbnail_url = a.absUrl("src").ifEmpty { a.selectFirst("img")?.absUrl("src") }
            }
        }.distinctBy { it.url }
        val hasNext = document.selectFirst("a.next.page-numbers, a.pagination-next") != null
        return MangasPage(mangas, hasNext)
    }

    // ----- Search -----

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/?s=${query.encodeURLParameter()}&post_type=wp-manga" +
            if (page > 1) "&paged=$page" else ""
        return GET(url, headers)
    }

    override fun searchMangaParse(response: okhttp3.Response): MangasPage = parseMangaList(response.asJsoup())

    // ----- Manga details -----

    override fun mangaDetailsRequest(manga: SManga) = GET(baseUrl + manga.url, headers)

    override fun mangaDetailsParse(response: okhttp3.Response): SManga {
        val document = response.asJsoup()
        val manga = SManga.create()
        val rawTitle = document.selectFirst("title")?.text().orEmpty()
        manga.title = rawTitle.substringBefore(" – ").substringBefore(" - ").trim().ifEmpty { rawTitle }
        manga.thumbnail_url = document.selectFirst("meta[property=og:image]")?.attr("content")
            ?.cleanTemplate()
            ?: document.selectFirst(".summary_image img, .tab-thumb img")?.absUrl("src")?.cleanTemplate()
        manga.description = document.selectFirst("meta[name=description]")?.attr("content")
            ?.cleanTemplate()
            ?: document.selectFirst(".summary_content, .post-content, .description")?.text()?.cleanTemplate()
        val statusText = (manga.description ?: "") + " " + (
            document.selectFirst(".post-content_item, .summary_content")?.text().cleanTemplate().orEmpty()
            )
        manga.status = when {
            statusText.contains("completed", true) -> SManga.COMPLETED
            statusText.contains("ongoing", true) -> SManga.ONGOING
            else -> SManga.UNKNOWN
        }
        return manga
    }

    // ----- Chapters (JS-loaded via POST ajax/chapters) -----

    override fun chapterListRequest(manga: SManga): Request {
        val url = baseUrl + manga.url + "ajax/chapters/?t=1"
        return Request.Builder()
            .url(url)
            .post("".toRequestBody())
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Referer", baseUrl + manga.url)
            .build()
    }

    override fun chapterListParse(response: okhttp3.Response): List<SChapter> {
        val document = response.asJsoup()
        return document.select("ul.main.version-chap li.wp-manga-chapter a, ul.main li.wp-manga-chapter a")
            .mapNotNull { a ->
                val href = a.absUrl("href")
                if (!href.contains("/episode-")) return@mapNotNull null
                SChapter.create().apply {
                    url = href.removePrefix(baseUrl)
                    name = a.text().trim().ifBlank { href.substringAfterLast("/").removeSuffix("/") }
                    chapter_number = CHAPTER_NUM_REGEX.find(name)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                }
            }
            .reversed()
    }

    // ----- Pages -----

    override fun pageListRequest(chapter: SChapter) = GET(baseUrl + chapter.url, headers)

    override fun pageListParse(response: okhttp3.Response): List<Page> {
        val document = response.asJsoup()
        val imagePattern = Regex("""\.(jpg|jpeg|png|webp)(\?.*)?$""", RegexOption.IGNORE_CASE)
        val pages = mutableListOf<Page>()
        var index = 0
        document.select("img.wp-manga-chapter-img, .reading-content img").forEach { img ->
            val src = img.attr("data-src").ifEmpty { img.attr("src") }.ifBlank { return@forEach }
            if (!imagePattern.containsMatchIn(src)) return@forEach
            pages.add(Page(index++, "", src))
        }
        return pages
    }

    override fun imageRequest(page: Page): Request {
        return GET(page.imageUrl!!, headersBuilder().add("Referer", "$baseUrl/").build())
    }

    override fun imageUrlParse(response: okhttp3.Response): String = throw UnsupportedOperationException()

    override fun getFilterList() = FilterList()

    private fun String.encodeURLParameter(): String =
        java.net.URLEncoder.encode(this, "UTF-8")

    private fun String?.cleanTemplate(): String? {
        val value = this?.trim().orEmpty()
        return if (value.isBlank() || value.contains("\${")) null else value
    }

    private fun okhttp3.Response.asJsoup(): Document =
        org.jsoup.Jsoup.parse(body?.string().orEmpty())

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        private val CHAPTER_NUM_REGEX = Regex("""(\d+)""")
    }
}
