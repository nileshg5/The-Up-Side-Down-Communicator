package com.example.theupsidedowncommunicator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.theupsidedowncommunicator.ui.theme.RetroBlack
import com.example.theupsidedowncommunicator.ui.theme.RetroCyan
import com.example.theupsidedowncommunicator.ui.theme.RetroRed
import com.example.theupsidedowncommunicator.ui.theme.RetroTeal
import com.example.theupsidedowncommunicator.ui.theme.TheUpsideDownCommunicatorTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TheUpsideDownCommunicatorTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = RetroBlack) {
                    DimensionDecoderApp()
                }
            }
        }
    }
}

@Composable
fun DimensionDecoderApp() {
    var screen by remember { mutableStateOf("LOGIN") }
    var currentUserId by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(EncodeMode.MORSE) }
    var sanity by remember { mutableStateOf(100f) }
    var isPossessed by remember { mutableStateOf(false) }
    var binaryData by remember { mutableStateOf(listOf<String>()) }
    var playbackMode by remember { mutableStateOf(EncodeMode.MORSE) }
    
    var broadcastMessages by remember { mutableStateOf(listOf<MessageRecord>()) }

    // Background Flicker Animation
    val infiniteTransition = rememberInfiniteTransition(label = "bg_flicker")
    val bgAlpha by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_alpha"
    )

    // Handle Back Button
    BackHandler(enabled = screen != "LOGIN" || isPossessed) {
        if (isPossessed) {
            // Cannot escape possession via back button
        } else if (screen == "INPUT") {
            screen = "LOGIN"
        } else {
            screen = "INPUT"
        }
    }

    // Sanity Drain
    LaunchedEffect(isPossessed) {
        while (!isPossessed) {
            delay(2000)
            if (sanity > 0) {
                sanity -= 1f
            } else {
                isPossessed = true
            }
        }
    }

    // Global Listener for Broadcasts
    LaunchedEffect(Unit) {
        FirebaseMessaging.fetchBroadcasts { list ->
            broadcastMessages = list
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(RetroBlack.copy(alpha = bgAlpha))
        .drawBehind {
            drawRect(
                Brush.radialGradient(
                    colors = listOf(RetroTeal.copy(alpha = 0.3f), Color.Transparent),
                    center = center,
                    radius = size.maxDimension / 1.5f
                )
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Header(
                sanity = sanity, 
                showBack = screen != "LOGIN" && screen != "INPUT",
                onBack = { screen = "INPUT" },
                userId = currentUserId,
                onBroadcastClick = { screen = "BROADCAST" }
            )
            
            Box(modifier = Modifier.weight(1f)) {
                when (screen) {
                    "LOGIN" -> LoginScreen { id ->
                        currentUserId = id
                        screen = "INPUT"
                    }
                    "INPUT" -> InputScreen(
                        message = message,
                        onMessageChange = { message = it },
                        selectedMode = selectedMode,
                        onModeSelected = { selectedMode = it },
                        onTransmit = {
                            FirebaseMessaging.sendBroadcast(currentUserId, message, selectedMode)
                            binaryData = DimensionLogic.encryptMessage(message)
                            playbackMode = selectedMode
                            screen = "PLAYBACK"
                        },
                        onGoToBroadcast = { screen = "BROADCAST" }
                    )
                    "BROADCAST" -> BroadcastScreen(
                        messages = broadcastMessages,
                        onPlaySignal = { msg ->
                            binaryData = DimensionLogic.encryptMessage(msg.content)
                            playbackMode = EncodeMode.valueOf(msg.mode)
                            screen = "PLAYBACK"
                        }
                    )
                    "PLAYBACK" -> PlaybackScreen(
                        binaryData = binaryData,
                        mode = playbackMode,
                        onFinished = { screen = "BROADCAST" }
                    )
                }
            }
        }

        StaticNoiseOverlay()
        ScanlineOverlay()

        if (isPossessed) {
            PossessedOverlay(onRecovered = {
                isPossessed = false
                sanity = 100f
            })
        }
    }
}

@Composable
fun LoginScreen(onLogin: (String) -> Unit) {
    var id by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("> IDENTIFY YOURSELF:", color = RetroCyan, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(16.dp))
        BasicTextField(
            value = id,
            onValueChange = { id = it },
            textStyle = TextStyle(color = RetroCyan, fontFamily = FontFamily.Monospace, fontSize = 24.sp, textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth().border(1.dp, RetroCyan).padding(16.dp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(RetroCyan)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            "CONNECT",
            modifier = Modifier.clickable { if(id.isNotEmpty()) onLogin(id) }.padding(16.dp),
            color = RetroCyan,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun Header(sanity: Float, showBack: Boolean = false, onBack: () -> Unit = {}, userId: String = "", onBroadcastClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showBack) {
                    Text(
                        "< BACK ",
                        modifier = Modifier.clickable { onBack() }.padding(end = 8.dp),
                        color = RetroCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "DIMENSION DECODER v1983",
                    color = RetroCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (userId.isNotEmpty()) {
                    Text("ID: $userId", color = RetroCyan.copy(alpha = 0.5f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "[ BROADCAST FEED ]",
                    modifier = Modifier.clickable { onBroadcastClick() },
                    color = RetroCyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                )
            }
        }
        
        SanityMeter(sanity)
    }
}

@Composable
fun SanityMeter(sanity: Float) {
    Column(horizontalAlignment = Alignment.End) {
        Text("SANITY", color = RetroCyan, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(10.dp)
                .border(1.dp, RetroCyan)
                .padding(1.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(sanity / 100f)
                    .background(if (sanity < 30) RetroRed else RetroCyan)
            )
        }
    }
}

@Composable
fun InputScreen(
    message: String,
    onMessageChange: (String) -> Unit,
    selectedMode: EncodeMode,
    onModeSelected: (EncodeMode) -> Unit,
    onTransmit: () -> Unit,
    onGoToBroadcast: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))
        
        Text("> BROADCAST MESSAGE:", color = RetroCyan, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
        BasicTextField(
            value = message,
            onValueChange = onMessageChange,
            textStyle = TextStyle(color = RetroCyan, fontFamily = FontFamily.Monospace, fontSize = 20.sp, textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth().border(1.dp, RetroCyan).padding(12.dp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(RetroCyan),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.Center) {
                    if (message.isEmpty()) Text("TYPE MESSAGE...", color = RetroCyan.copy(alpha = 0.2f))
                    innerTextField()
                }
            }
        )

        Text("> SIGNAL TYPE:", color = RetroCyan, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EncodeMode.values().forEach { mode ->
                Box(
                    modifier = Modifier
                        .border(1.dp, if (selectedMode == mode) RetroCyan else RetroCyan.copy(alpha = 0.3f))
                        .clickable { onModeSelected(mode) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(mode.name, color = if (selectedMode == mode) RetroCyan else RetroCyan.copy(alpha = 0.3f), fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            "TRANSMIT TO ALL",
            modifier = Modifier.clickable { onTransmit() }.padding(16.dp),
            color = RetroCyan,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        
        Text(
            "[ VIEW ALL BROADCASTS ]",
            modifier = Modifier.clickable { onGoToBroadcast() }.padding(8.dp),
            color = RetroCyan.copy(alpha = 0.7f),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

@Composable
fun BroadcastScreen(
    messages: List<MessageRecord>,
    onPlaySignal: (MessageRecord) -> Unit
) {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("> GLOBAL BROADCAST FEED (ALL USERS):", color = RetroCyan, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(messages) { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, RetroCyan.copy(alpha = 0.3f))
                        .clickable { onPlaySignal(msg) }
                        .padding(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(msg.from, color = RetroCyan, fontFamily = FontFamily.Monospace, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("SIGNAL: ${msg.mode}", color = RetroCyan.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }
                        Text(dateFormat.format(Date(msg.timestamp)), color = RetroCyan.copy(alpha = 0.4f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun PlaybackScreen(
    binaryData: List<String>,
    mode: EncodeMode,
    onFinished: () -> Unit
) {
    var currentBit by remember { mutableStateOf(' ') }
    var currentChunk by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }

    LaunchedEffect(binaryData) {
        for (chunk in binaryData) {
            currentChunk = chunk
            for (bit in chunk) {
                currentBit = bit
                active = true
                delay(if (bit == '1') 600 else 250)
                active = false
                delay(150)
            }
            delay(500)
        }
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (mode) {
            EncodeMode.MORSE -> {
                Canvas(modifier = Modifier.size(100.dp)) {
                    if (active) drawCircle(color = RetroCyan, radius = if (currentBit == '1') 50.dp.toPx() else 20.dp.toPx(), alpha = 0.8f)
                }
            }
            EncodeMode.COLOR -> {
                Box(modifier = Modifier.fillMaxSize().background(if (active) (if (currentBit == '1') RetroRed.copy(alpha = 0.5f) else RetroCyan.copy(alpha = 0.5f)) else Color.Transparent))
            }
            EncodeMode.BEEP -> {
                Text(if (active) "POISON ON\nTHE INSIDE" else "", color = RetroCyan, fontSize = 24.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
            }
            EncodeMode.GRID -> {
                if (active) SymbolGrid(currentChunk)
            }
        }
    }
}

@Composable
fun SymbolGrid(chunk: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        StaticPatternRow(modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp))
        StaticPatternRow(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp))
    }
}

@Composable
fun StaticPatternRow(modifier: Modifier = Modifier) {
    val seed = remember { Random.nextInt() }
    Canvas(modifier = modifier.fillMaxWidth().height(60.dp)) {
        val rows = 4
        val cols = 40
        val cellW = size.width / cols
        val cellH = size.height / rows
        val rand = Random(seed)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (rand.nextBoolean()) {
                    drawRect(color = RetroCyan.copy(alpha = 0.6f), topLeft = Offset(c * cellW, r * cellH), size = androidx.compose.ui.geometry.Size(cellW * 0.8f, cellH * 0.6f))
                }
            }
        }
    }
}

@Composable
fun StaticNoiseOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "noise")
    val offset by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 100f, animationSpec = infiniteRepeatable(animation = tween(100, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "offset")
    Canvas(modifier = Modifier.fillMaxSize().alpha(0.1f)) {
        val rand = Random(offset.toInt())
        for (i in 0..500) {
            drawCircle(color = RetroCyan, radius = 1f, center = Offset(rand.nextFloat() * size.width, rand.nextFloat() * size.height))
        }
    }
}

@Composable
fun ScanlineOverlay() {
    Canvas(modifier = Modifier.fillMaxSize().alpha(0.2f)) {
        val lineSpacing = 3.dp.toPx()
        for (y in 0..size.height.toInt() step lineSpacing.toInt()) {
            drawLine(color = Color.Black, start = Offset(0f, y.toFloat()), end = Offset(size.width, y.toFloat()), strokeWidth = 1.5f)
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "flicker")
    val flickerAlpha by infiniteTransition.animateFloat(initialValue = 0.01f, targetValue = 0.04f, animationSpec = infiniteRepeatable(animation = tween(40, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "alpha")
    Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = flickerAlpha)))
}

@Composable
fun PossessedOverlay(onRecovered: () -> Unit) {
    var konamiInput by remember { mutableStateOf(listOf<String>()) }
    val konamiCode = listOf("UP", "UP", "DOWN", "DOWN", "LEFT", "RIGHT", "LEFT", "RIGHT", "B", "A")
    val shakeTransition = rememberInfiniteTransition(label = "shake")
    val shakeOffset by shakeTransition.animateFloat(initialValue = -8f, targetValue = 8f, animationSpec = infiniteRepeatable(animation = tween(20, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "offset")
    Box(modifier = Modifier.fillMaxSize().background(RetroRed.copy(alpha = 0.2f)).graphicsLayer(translationX = shakeOffset, translationY = shakeOffset).drawBehind {
        for (i in 0..15) {
            val y = Random.nextFloat() * size.height
            drawLine(RetroRed.copy(alpha = 0.5f), Offset(0f, y), Offset(size.width, y + Random.nextFloat() * 10), strokeWidth = Random.nextFloat() * 8)
        }
    }.clickable(enabled = false) {}) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("SYSTEM COMPROMISED", color = Color.White, fontSize = 24.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(48.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RecoveryButton("UP") { konamiInput = (konamiInput + "UP").takeLast(10) }
                Row {
                    RecoveryButton("LEFT") { konamiInput = (konamiInput + "LEFT").takeLast(10) }
                    Spacer(modifier = Modifier.width(32.dp))
                    RecoveryButton("RIGHT") { konamiInput = (konamiInput + "RIGHT").takeLast(10) }
                }
                RecoveryButton("DOWN") { konamiInput = (konamiInput + "DOWN").takeLast(10) }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row {
                RecoveryButton("B") { konamiInput = (konamiInput + "B").takeLast(10) }
                Spacer(modifier = Modifier.width(16.dp))
                RecoveryButton("A") { konamiInput = (konamiInput + "A").takeLast(10) }
            }
        }
    }
    LaunchedEffect(konamiInput) { if (konamiInput == konamiCode) onRecovered() }
}

@Composable
fun RecoveryButton(label: String, onClick: () -> Unit) {
    Box(modifier = Modifier.padding(4.dp).border(1.dp, Color.White).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(label, color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}
