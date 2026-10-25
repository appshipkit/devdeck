package io.devdeck.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.devdeck.logs.DeckLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter

/**
 * A paged feed that never ends: reaching the bottom loads 20 more items after a short delay, like a real
 * "load more" list. Use it to try *Take a long screenshot* on endless content.
 */
class EndlessFeedActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) { EndlessFeed() }
            }
        }
    }
}

@Composable
private fun EndlessFeed() {
    var count by remember { mutableIntStateOf(PAGE_SIZE) }
    var pages by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            last >= info.totalItemsCount - LOAD_AHEAD
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { nearEnd }.filter { it }.collect {
            if (loading) return@collect
            loading = true
            delay(LOAD_DELAY_MILLIS)
            count += PAGE_SIZE
            pages++
            loading = false
            DeckLogger.log("Feed page $pages loaded", tag = "Feed", payload = "$count items")
        }
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Text(
            "Endless feed",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(count, key = { it }) { index -> FeedItem(index + 1) }
            item(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
private fun FeedItem(number: Int) {
    // Each item gets its own colour and number, so seams or repeated pieces in a long screenshot are easy to spot.
    val hue = (number * 37 % 360).toFloat()
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(48.dp).background(Color.hsv(hue, 0.55f, 0.85f), RoundedCornerShape(8.dp)))
        Column {
            Text("Item $number", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Page ${(number - 1) / PAGE_SIZE + 1} · loaded as you scroll", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private const val PAGE_SIZE = 20
private const val LOAD_AHEAD = 3
private const val LOAD_DELAY_MILLIS = 800L
