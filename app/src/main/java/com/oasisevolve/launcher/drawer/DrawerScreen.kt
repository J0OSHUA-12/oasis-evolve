package com.oasisevolve.launcher.drawer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oasisevolve.launcher.LauncherState
import com.oasisevolve.launcher.LauncherViewModel
import com.oasisevolve.launcher.system.AppEntry
import com.oasisevolve.launcher.ui.SearchField
import kotlinx.coroutines.launch

/** Mínimo de letras para la búsqueda deliberada: buscar una app oculta debe requerir intención. */
private const val DELIBERATE_MIN_CHARS = 3

/** Lista de nombres. Por diseño nunca muestra iconos ni widgets, con independencia de los ajustes. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrawerScreen(
    state: LauncherState,
    viewModel: LauncherViewModel,
    onLongPress: (AppEntry) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var deliberate by remember { mutableStateOf(false) }

    val results = if (deliberate) {
        if (query.trim().length < DELIBERATE_MIN_CHARS) emptyList()
        else filterBySearch(state.allApps.filterNot { state.level(it).inDrawer }, query) { it.label }
    } else {
        filterBySearch(state.allApps.filter { state.level(it).inDrawer }, query) { it.label }
    }
    val letters = remember(results, query) {
        if (query.isBlank()) results.map { railLetter(it.label) }.distinct() else emptyList()
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 24.dp, end = 8.dp, top = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = if (deliberate) "Búsqueda deliberada" else "Buscar apps",
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = {
                deliberate = !deliberate
                query = ""
            }) {
                Text(if (deliberate) "Volver" else "Ocultas")
            }
        }
        if (deliberate) {
            Text(
                "Escribe al menos $DELIBERATE_MIN_CHARS letras para buscar entre tus apps ocultas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Row(
            Modifier
                .weight(1f)
                .padding(top = 8.dp),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                items(results, key = { it.key }) { app ->
                    Text(
                        text = if (app.isWorkProfile) "${app.label} (trabajo)" else app.label,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { viewModel.open(app) },
                                onLongClick = { onLongPress(app) },
                            )
                            .padding(vertical = 11.dp),
                    )
                }
            }
            if (letters.size > 1) {
                LetterRail(letters) { letter ->
                    val index = results.indexOfFirst { railLetter(it.label) == letter }
                    if (index >= 0) scope.launch { listState.scrollToItem(index) }
                }
            }
        }
    }
}

@Composable
private fun LetterRail(letters: List<Char>, onLetter: (Char) -> Unit) {
    var height by remember { mutableIntStateOf(1) }
    fun pick(y: Float) {
        val index = (y / height * letters.size).toInt().coerceIn(letters.indices)
        onLetter(letters[index])
    }
    Box(
        Modifier
            .width(28.dp)
            .fillMaxHeight()
            .onSizeChanged { height = it.height.coerceAtLeast(1) }
            .pointerInput(letters) { detectTapGestures { pick(it.y) } }
            .pointerInput(letters) {
                detectDragGestures { change, _ ->
                    change.consume()
                    pick(change.position.y)
                }
            },
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            letters.forEach { letter ->
                Text(
                    letter.toString(),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                )
            }
        }
    }
}
