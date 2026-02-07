package com.example.theupsidedowncommunicator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.theupsidedowncommunicator.ui.theme.RetroBlack
import com.example.theupsidedowncommunicator.ui.theme.RetroGreen
import com.example.theupsidedowncommunicator.ui.theme.RetroRed
import com.example.theupsidedowncommunicator.ui.theme.TheUpsideDownCommunicatorTheme
import kotlinx.coroutines.delay
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
    var screen by remember { mutableStateOf("INPUT") }
    var message by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(EncodeMode.MORSE) }
    var sanity by remember { mutableStateOf(100f) }
    var isPossessed by remember { mutableStateOf(false) }
    var binaryData by remember { mutableStateOf(listOf<String>()) }

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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Header(sanity)
            
            Box(modifier = Modifier.weight(1f)) {
                when (screen) {
                    "INPUT" -> InputScreen(
                        message = message,
                        onMessageChange = { message = it },
                        selectedMode = selectedMode,
                        onModeSelected = { selectedMode = it },
                        onTransmit = {
                            binaryData = DimensionLogic.encryptMessage(message)
                            screen = "PLAYBACK"
                        }
                    )
                    "PLAYBACK" -> PlaybackScreen(
                        binaryData = binaryData,
                        mode = selectedMode,
                        onFinished = { screen = "INPUT" }
                    )
                }
            }
        }

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
fun Header(sanity: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "DIMENSION DECODER v1983",
            color = RetroGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        
        SanityMeter(sanity)
    }
}

@Composable
fun SanityMeter(sanity: Float) {
    Column(horizontalAlignment = Alignment.End) {
        Text("SANITY", color = RetroGreen, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(10.dp)
                .border(1.dp, RetroGreen)
                .padding(1.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(sanity / 100f)
                    .background(if (sanity < 30) RetroRed else RetroGreen)
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
    onTransmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "> ENTER MESSAGE FOR THE VOID:",
            color = RetroGreen,
            fontFamily = FontFamily.Monospace
        )
        
        BasicTextField(
            value = message,
            onValueChange = onMessageChange,
            textStyle = TextStyle(color = RetroGreen, fontFamily = FontFamily.Monospace, fontSize = 20.sp),
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .border(1.dp, RetroGreen)
                .padding(8.dp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(RetroGreen)
        )

        Text("> SELECT SIGNAL MODE:", color = RetroGreen, fontFamily = FontFamily.Monospace)
        
        EncodeMode.values().forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onModeSelected(mode) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (selectedMode == mode) "[X] $mode" else "[ ] $mode",
                    color = RetroGreen,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onTransmit,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = RetroGreen, contentColor = RetroBlack),
            shape = androidx.compose.ui.graphics.RectangleShape
        ) {
            Text("TRANSMIT", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
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
            delay(500) // Byte gap
        }
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (mode) {
            EncodeMode.MORSE -> {
                Canvas(modifier = Modifier.size(150.dp)) {
                    if (active) {
                        drawCircle(color = RetroGreen, radius = if (currentBit == '1') 75.dp.toPx() else 40.dp.toPx())
                    }
                }
            }
            EncodeMode.COLOR -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (active) (if (currentBit == '1') RetroRed else RetroGreen) else RetroBlack)
                )
            }
            EncodeMode.BEEP -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (active) "((( BEEP )))" else "...",
                        color = RetroGreen,
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            EncodeMode.GRID -> {
                if (active) {
                    SymbolGrid(currentChunk)
                }
            }
        }
        
        Text(
            "TRANSMITTING...",
            modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
            color = RetroGreen,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun SymbolGrid(chunk: String) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.size(180.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(9) { index ->
            // Use bits of chunk to light up grid (up to 8 bits)
            val isLit = if (index < chunk.length) chunk[index] == '1' else false
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .aspectRatio(1f)
                    .background(if (isLit) RetroGreen else Color.Transparent)
                    .border(1.dp, RetroGreen)
            )
        }
    }
}

@Composable
fun ScanlineOverlay() {
    Canvas(modifier = Modifier.fillMaxSize().alpha(0.15f)) {
        val lineSpacing = 4.dp.toPx()
        for (y in 0..size.height.toInt() step lineSpacing.toInt()) {
            drawLine(
                color = Color.Black,
                start = Offset(0f, y.toFloat()),
                end = Offset(size.width, y.toFloat()),
                strokeWidth = 2f
            )
        }
    }
    
    // Flicker effect
    val infiniteTransition = rememberInfiniteTransition(label = "flicker")
    val flickerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.02f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = flickerAlpha)))
}

@Composable
fun PossessedOverlay(onRecovered: () -> Unit) {
    var konamiInput by remember { mutableStateOf(listOf<String>()) }
    val konamiCode = listOf("UP", "UP", "DOWN", "DOWN", "LEFT", "RIGHT", "LEFT", "RIGHT", "B", "A")
    
    val shakeTransition = rememberInfiniteTransition(label = "shake")
    val shakeOffset by shakeTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(30, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RetroRed.copy(alpha = 0.3f))
            .graphicsLayer(translationX = shakeOffset, translationY = shakeOffset)
            .drawBehind {
                // Glitch lines
                for (i in 0..10) {
                    val y = Random.nextFloat() * size.height
                    drawLine(
                        RetroRed,
                        Offset(0f, y),
                        Offset(size.width, y + Random.nextFloat() * 20),
                        strokeWidth = Random.nextFloat() * 10
                    )
                }
            }
            .clickable(enabled = false) {} // Intercept clicks
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "DIMENSIONAL INTERFERENCE DETECTED",
                color = Color.White,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // D-Pad for recovery
            Text("RESTORE CONNECTION SEQUENCE:", color = Color.White, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RecoveryButton("UP") { konamiInput = (konamiInput + "UP").takeLast(10) }
                Row {
                    RecoveryButton("LEFT") { konamiInput = (konamiInput + "LEFT").takeLast(10) }
                    Spacer(modifier = Modifier.width(48.dp))
                    RecoveryButton("RIGHT") { konamiInput = (konamiInput + "RIGHT").takeLast(10) }
                }
                RecoveryButton("DOWN") { konamiInput = (konamiInput + "DOWN").takeLast(10) }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                RecoveryButton("B") { konamiInput = (konamiInput + "B").takeLast(10) }
                Spacer(modifier = Modifier.width(16.dp))
                RecoveryButton("A") { konamiInput = (konamiInput + "A").takeLast(10) }
            }
        }
    }
    
    LaunchedEffect(konamiInput) {
        if (konamiInput == konamiCode) {
            onRecovered()
        }
    }
}

@Composable
fun RecoveryButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = RetroRed),
        shape = androidx.compose.ui.graphics.RectangleShape,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(label, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}
