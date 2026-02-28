package com.example.cuesheetapptest

import android.graphics.Paint.Align
import android.os.Bundle
import android.view.Window.OnFrameMetricsAvailableListener
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

// Compose 기초 및 레이아웃 관련
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cuesheetapptest.ui.theme.*

// 상호작용 관련
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.res.colorResource

// 파일 받아오기
import android.net.Uri // URIIIIIII???????????? URI는 또 첨임다...
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext 
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import android.widget.Toast

val MainRed = Color(0xFFC25050)
val BgGray = Color(0xFFD9D9D9)
val ItemPink = Color(0xFFF2C2C2)

data class Song(
    val title: String,
    val defaultSequence: MutableList<String>,
    val lyricsMap: Map<String, String>
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CueSheetApp()
        }
    }
}

@Composable
fun CueSheetApp() {
    var currentScreen by remember { mutableStateOf(0) }

    val importedSongs = remember { mutableStateListOf<Song>() }

    var currentLyricsMap by remember { mutableStateOf<Map<String, String>>(emptyMap())}
    val currentSequence = remember { mutableStateListOf<String>() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BgGray
    ) {
        when (currentScreen) {
            0 -> MainStartScreen(
                onExistingsClick = { currentScreen = 1 },
                onFileImported = { newSong ->
                    importedSongs.add(newSong)
                    currentScreen = 1 }
            )
            1 -> ImportedListScreen(
                songs = importedSongs,
                onBackClick = { currentScreen = 0 },
                onItemClick = { clickedIndex ->
                    currentSequence.clear()
                    currentSequence.addAll(importedSongs[clickedIndex].defaultSequence)
                    currentLyricsMap = importedSongs[clickedIndex].lyricsMap

                    currentScreen = 2
                }
            )

            2 -> CueDetailScreen(
                cueSequence = currentSequence,
                lyricsMap = currentLyricsMap,
                onEditClick = { currentScreen = 3 },
                onBackClick = { currentScreen = 1 }
            )

            3 -> CueEditScreen(
                cueSequence = currentSequence,
                onDoneClick = { currentScreen = 2 }
            )
        }
    }
}

@Composable
fun MainStartScreen(onExistingsClick: () -> Unit, onFileImported: (Song) -> Unit) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val inputStream = context.contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.use { it.readText() }

            val jsonObject = JSONObject(jsonString)
            val title = jsonObject.getString("title")

            val seqArray = jsonObject.getJSONArray("defaultSequence")
            val sequence = mutableListOf<String>()
            for (i in 0 until seqArray.length()) { sequence.add(seqArray.getString(i)) }

            val lyricsObj = jsonObject.getJSONObject("lyrics")
            val lyricsMap = mutableMapOf<String, String>()
            val keys = lyricsObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                lyricsMap[key] = lyricsObj.getString(key)
            }
            onFileImported(Song(title, sequence, lyricsMap))
            Toast.makeText(context, "$title loaded!", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ActionButton(text = "Import", onClick = {launcher.launch("*/*")})

        Spacer(modifier = Modifier.height(32.dp))

        ActionButton(text = "Existings", onClick = onExistingsClick)
    }
}

@Composable
fun ImportedListScreen(songs: List<Song>, onBackClick: () -> Unit, onItemClick: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(onClick = onBackClick) { Text("Close", color = MainRed)}

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(MainRed)
                .padding(20.dp)
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp) // 왜 spaceBy가 아니닌건데ㅔ에ㅔ에ㅔ
            ) {
                items(songs.size) { index ->
                    ImportedItemRow(
                        text = songs[index].title,
                        onClick = { onItemClick(index) }
                    )
                }
            }
        }
    }
}

@Composable
fun ActionButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = MainRed),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.size(width =160.dp, height = 60.dp)
    ) {
        Text(text = text, color = Color.White, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun ImportedItemRow(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable { onClick()},
        color = ItemPink,
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(text = text, color = MainRed, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun CueDetailScreen(onEditClick: () -> Unit, onBackClick: () -> Unit, cueSequence: List<String>, lyricsMap: Map<String, String>) {
    var currentIndex by remember { mutableStateOf(0) }

    if (currentIndex >= cueSequence.size) {
        currentIndex = maxOf(0, cueSequence.size - 1)
    }

    val currentCue = cueSequence.getOrNull(currentIndex) ?: "Empty"
    val rawLyrics = lyricsMap[currentCue] ?: "No Lyrics"
    val displayLyrics = rawLyrics.replace("/","\n")

    val prevCue = cueSequence.getOrNull(currentIndex - 1) ?: "Start"
    val nextCue = cueSequence.getOrNull(currentIndex + 1) ?: "End"
    val hotKeyText = "$prevCue -> [$currentCue] -> $nextCue"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 60.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(onClick = onBackClick) { Text("Back to List", color = MainRed)}

        RedCard(text = displayLyrics, modifier = Modifier.weight(1f))

        RedCard(
            text = "HotKey for Current Cue\n\n(Tap or Slide)\n\n$hotKeyText",
            modifier = Modifier
                .weight(1f)
                .clickable {
                    if (currentIndex < cueSequence.size - 1) currentIndex++ // 이건 진짜 신기하다 이런 형태도 되는구나
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()

                        if (dragAmount < -15f && currentIndex < cueSequence.size - 1) { // 추후 한번에 하나씩만 넘어가게 수정 요망
                            currentIndex++
                        } else if (dragAmount > 15f && currentIndex > 0) {
                            currentIndex--
                        }
                    }
                }
        )
        Button(
            onClick = onEditClick,
            colors = ButtonDefaults.buttonColors(containerColor = MainRed),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Edit", color = Color.White)
        }
    }
}

@Composable
fun RedCard(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MainRed)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CueEditScreen(
    cueSequence: MutableList<String>,
    onDoneClick: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var targetIndex by remember { mutableStateOf(-1) }
    var dialogMode by remember { mutableStateOf("") }

    val availableSections = listOf("V1","V2","V3","C","B","I","O") // 추후 기능 추가 요망 및 figma 디자인대로 수정 요망

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 60.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(onClick = onDoneClick) { Text("Save & Close", color=MainRed) }
        RedCard(text = "Tap and arrow(->) to Insert\n Tap a text(V1, C) to Edit/Delete", modifier = Modifier.weight(0.5f))

        Box(
            modifier = Modifier
                .weight(1.5f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MainRed)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cueSequence.forEachIndexed { index, cue ->
                    Text(
                        text = "[$cue]",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .clickable {
                                targetIndex = index
                                dialogMode = "EDIT"
                                showDialog = true
                            }
                            .padding(horizontal = 4.dp)
                    )

                    if (index < cueSequence.size - 1) {
                        Text(
                            text = "->",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .clickable {
                                    targetIndex = index
                                    dialogMode = "INSERT"
                                    showDialog = true
                                }
                                .padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (dialogMode == "INSERT") "Add next to here" else "Edit or Delete") },
            text = {
                Column {
                    availableSections.forEach { section ->
                        TextButton(
                            onClick = {
                                if (dialogMode == "INSERT") {
                                    cueSequence.add(targetIndex + 1, section)
                                } else if (dialogMode == "EDIT") {
                                    cueSequence[targetIndex] = section
                                }
                                showDialog = false
                            }
                        ) {
                            Text(section)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                if (dialogMode == "EDIT") {
                    TextButton(
                        onClick = {
                            cueSequence.removeAt(targetIndex)
                            showDialog = false
                        }
                    ) {
                        Text("Delete", color=Color.Red)
                    }
                }
            }
        )
    }
} // Kotlin 진짜 어색하네... :/