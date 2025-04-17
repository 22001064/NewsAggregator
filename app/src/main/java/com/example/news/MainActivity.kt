package com.example.news

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.pager.*
import com.google.accompanist.swiperefresh.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var darkTheme by remember { mutableStateOf(false) }
            MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
                NewsApp(darkTheme = darkTheme, onToggleTheme = { darkTheme = !darkTheme })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsApp(
    darkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: NewsViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NewsViewModel(context) as T
        }
    })

    val categories = listOf("General", "Business", "Technology", "Sports", "Health")
    val pagerState = rememberPagerState(initialPage = 0)
    val coroutineScope = rememberCoroutineScope()
    var showBookmarks by remember { mutableStateOf(false) }
    var selectedArticle by remember { mutableStateOf<Article?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("E-Press") },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                    IconButton(onClick = { showBookmarks = !showBookmarks }) {
                        Icon(Icons.Default.Bookmarks, contentDescription = "Bookmarks")
                    }
                }
            )
        }
    ) { innerPadding ->
        selectedArticle?.let { article ->
            AlertDialog(
                onDismissRequest = { selectedArticle = null },
                title = { Text(article.headline) },
                text = { Text(article.summary) },
                confirmButton = {
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse(article.link)
                        }
                        context.startActivity(intent)
                        selectedArticle = null
                    }) {
                        Text("Read More")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedArticle = null }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showBookmarks) {
            BookmarkScreen(viewModel, Modifier.padding(innerPadding), onClick = { selectedArticle = it })
        } else {
            Column(modifier = Modifier.padding(innerPadding)) {
                ScrollableTabRow(selectedTabIndex = pagerState.currentPage) {
                    categories.forEachIndexed { index, category ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = { Text(category) }
                        )
                    }
                }

                HorizontalPager(
                    count = categories.size,
                    state = pagerState,
                    key = { it }
                ) { page ->
                    var articles by remember { mutableStateOf<List<Article>>(emptyList()) }

                    LaunchedEffect(categories[page]) {
                        articles = viewModel.fetchArticles(categories[page])
                    }

                    SwipeRefreshList(
                        articles = articles,
                        viewModel = viewModel,
                        onClick = { selectedArticle = it }
                    )
                }
            }
        }
    }
}

@Composable
fun SwipeRefreshList(
    articles: List<Article>,
    viewModel: NewsViewModel,
    onClick: (Article) -> Unit
) {
    val context = LocalContext.current
    var isRefreshing by remember { mutableStateOf(false) }
    val refreshState = rememberSwipeRefreshState(isRefreshing)
    val coroutineScope = rememberCoroutineScope()

    SwipeRefresh(
        state = refreshState,
        onRefresh = {
            isRefreshing = true
            coroutineScope.launch {
                kotlinx.coroutines.delay(1000)
                isRefreshing = false
            }
        }
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(articles) { article ->
                ArticleItem(
                    article,
                    onBookmarkClick = { viewModel.bookmarkArticle(article) },
                    onShareClick = { shareArticle(context, article) },
                    onClick = { onClick(article) }
                )
            }
        }
    }
}

@Composable
fun ArticleItem(
    article: Article,
    onBookmarkClick: () -> Unit,
    onShareClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            AsyncImage(
                model = article.imageUrl.takeIf { it.isNotEmpty() },
                contentDescription = null,
                placeholder = painterResource(id = R.drawable.placeholder),
                error = painterResource(id = R.drawable.placeholder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = article.headline, style = MaterialTheme.typography.titleMedium, maxLines = 2)
            Text(
                text = article.summary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = article.source, style = MaterialTheme.typography.labelSmall)
                Row {
                    IconButton(onClick = onBookmarkClick) {
                        Icon(Icons.Default.Bookmark, contentDescription = "Bookmark")
                    }
                    IconButton(onClick = onShareClick) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            }
        }
    }
}

@Composable
fun BookmarkScreen(
    viewModel: NewsViewModel,
    modifier: Modifier = Modifier,
    onClick: (Article) -> Unit
) {
    val context = LocalContext.current
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(viewModel.bookmarkedArticles) { article ->
            ArticleItem(
                article,
                onBookmarkClick = { viewModel.bookmarkArticle(article) },
                onShareClick = { shareArticle(context, article) },
                onClick = { onClick(article) }
            )
        }
    }
}

class NewsViewModel(private val context: Context) : ViewModel() {
    private val _bookmarkedArticles = mutableStateListOf<Article>()
    val bookmarkedArticles: List<Article> get() = _bookmarkedArticles

    fun bookmarkArticle(article: Article) {
        if (_bookmarkedArticles.contains(article)) {
            _bookmarkedArticles.remove(article)
        } else {
            _bookmarkedArticles.add(article)
        }
    }

    suspend fun fetchArticles(category: String): List<Article> {
        val apiKey = "eb34601c1ee5800e3337ee83b4833b33"
        Log.d("KeyCheck", "Using API key: '$apiKey'")
        Log.d("GNewsDebug", "Sending API request with key: '${apiKey}'")
        Log.d("KEYCHECK", "API key length = ${apiKey.length}, value = '$apiKey'")

        return withContext(Dispatchers.IO) {
            try {
                val response = GNewsApi.service.getTopHeadlines(
                    category = category.lowercase(),
                    apiKey = apiKey
                )
                response.articles.map {
                    Article(
                        headline = it.title,
                        summary = it.description ?: "",
                        date = it.publishedAt,
                        link = it.url,
                        imageUrl = it.image ?: "",
                        source = it.source.name
                    )
                }
            } catch (e: Exception) {
                Log.e("NewsFetchError", "Failed to load articles from GNews", e)
                emptyList()
            }
        }
    }
}

fun shareArticle(context: Context, article: Article) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "${article.headline}\\n${article.link}")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}

data class Article(
    val headline: String,
    val summary: String,
    val date: String,
    val link: String,
    val imageUrl: String = "",
    val source: String = "Unknown Source"
)

// ---------------------------
// News API Integration
// ---------------------------

data class GNewsResponse(
    val articles: List<GNewsArticle>
)

data class GNewsArticle(
    val title: String,
    val description: String?,
    val publishedAt: String,
    val url: String,
    val image: String?,
    val source: GNewsSource
)

data class GNewsSource(
    val name: String,
    val url: String
)

interface GNewsApiService {
    @GET("top-headlines")
    suspend fun getTopHeadlines(
        @Query("category") category: String,
        @Query("country") country: String = "gb",
        @Query("lang") language: String = "en",
        @Query("token") apiKey: String
    ): GNewsResponse
}

object GNewsApi {
    private const val BASE_URL = "https://gnews.io/api/v4/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    val service: GNewsApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GNewsApiService::class.java)
}