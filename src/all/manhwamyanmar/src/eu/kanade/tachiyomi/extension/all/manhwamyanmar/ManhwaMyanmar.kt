package eu.kanade.tachiyomi.extension.all.manhwamyanmar

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asJsoup
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.regex.Pattern

class ManhwaMyanmar : HttpSource() {

    override val name = "Manhwa Myanmar (Adult)"
    override val baseUrl = "https://adult.manhwamyanmar.com"
    override val lang = "my"
    override val supportsLatest = true

    private val chapterBaseHost = "https://18.manhwamyanmar.com"
    private val imageHostPattern = Pattern.compile("""https?://[^\s"']+\.(?:jpg|jpeg|png|webp)""", Pattern.CASE_INSENSITIVE)

    override val headers = headersBuilder()
        .add("User-Agent", USER_AGENT)
        .add("Accept-Language", "en-US,en;q=0.9")
        .build()

    // ----- Listing (popular / latest) -----

    override fun popularMangaRequest(page: Int): Request {
        val url = if (page == 1) baseUrl else "$baseUrl/page/$page/"
        return GET(url, headers)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val entries = document.select(".gridmini-grid-post").mapNotNull { it.toSManga() }
        val hasNext = document.selectFirst("a.next.page-numbers") != null
        return MangasPage(entries, hasNext)
    }

    override fun latestMangaRequest(page: Int) = popularMangaRequest(page)
    override fun latestMangaParse(response: Response) = popularMangaParse(response)

    private fun Element.toSManga(): SManga? {
        val link = selectFirst("a.gridmini-grid-post-thumbnail-link") ?: return null
        val titleEl = selectFirst(".gridmini-grid-post-title a")
        return SManga.create().apply {
            url = link.attr("href").removePrefix(baseUrl).removeSuffix("/") + "/"
            title = titleEl?.text()?.trim()
                ?: link.attr("title").removePrefix("Permanent Link to ").trim()
            thumbnail_url = selectFirst("img")?.let { it.attr("data-src").ifEmpty { it.attr("src") } }
        }
    }

    // ----- Search -----

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val urlBuilder = baseUrl.toHttpUrl().newBuilder().apply {
            if (page > 1) {
                addPathSegment("page")
                addPathSegment(page.toString())
            }
            addQueryParameter("s", query)
        }
        return GET(urlBuilder.build(), headers)
    }

    override fun searchMangaParse(response: Response) = popularMangaParse(response)

    // ----- Manga details -----

    override fun mangaDetailsParse(document: Document): SManga {
        val content = document.selectFirst(".entry-content")
        val manga = SManga.create()
        manga.title = document.selectFirst("h1.post-title a")?.text()
            ?: document.selectFirst("h1.post-title")?.text()
            ?: ""
        manga.thumbnail_url = content?.selectFirst("img")?.attr("src")
        manga.genre = document.select(".post-tags a").joinToString { it.text() }

        val paragraphs = content?.select("p").orEmpty()
            .map { it.text().trim() }
            .filter { it.isNotEmpty() }
        manga.description = paragraphs.joinToString("\n\n")

        val fullText = content?.text().orEmpty()
        manga.status = when {
            fullText.contains("completed", true) -> SManga.COMPLETED
            fullText.contains("ongoing", true) -> SManga.ONGOING
            else -> SManga.UNKNOWN
        }
        return manga
    }

    // ----- Chapters -----

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        return document.select("a[href*='18.manhwamyanmar.com']")
            .mapNotNull { a ->
                val href = a.attr("href")
                if (!href.contains("18.manhwamyanmar.com")) return@mapNotNull null
                val name = a.selectFirst("button")?.text()?.trim() ?: a.text().trim()
                if (name.isEmpty()) return@mapNotNull null
                SChapter.create().apply {
                    url = href
                    this.name = name
                    chapter_number = CHAPTER_NUM_REGEX.find(name)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                }
            }
            .reversed()
    }

    // ----- Pages -----

    override fun pageListRequest(chapter: SChapter): Request = GET(chapter.url, headers)

    override fun pageListParse(document: Document): List<Page> {
        val pages = mutableListOf<String>()
        for (img in document.select("img")) {
            val src = img.attr("data-src").ifEmpty { img.attr("src") }
            if (src.isBlank() || src.contains("lazy_placeholder", true)) continue
            if (imageHostPattern.matcher(src).matches()) pages.add(src)
        }
        return pages.distinct().mapIndexed { index, url -> Page(index, "", url) }
    }

    override fun imageRequest(page: Page): Request {
        return GET(page.imageUrl!!, headersBuilder().add("Referer", chapterBaseHost).build())
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()

    override fun getFilterList() = FilterList()

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        private val CHAPTER_NUM_REGEX = Regex("""(\d+)""")
    }
}
