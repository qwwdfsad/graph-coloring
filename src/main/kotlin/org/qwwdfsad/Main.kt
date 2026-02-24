package org.qwwdfsad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

// God bless IJ colorpicker
private val vertexColors = listOf(
    Color(0xFF5D42F4), // blue
    Color(0xFFEA4335), // red
    Color(0xFF34A853), // green
    Color(0xFFFBBC05), // yellow
    Color(0xFF9C27B0), // purple
    Color(0xFFFF6D00), // orange
    Color(0xFF00BCD4), // cyan
    Color(0xFFE91E63), // pink
)

// Note: all UI is mostly CC-generated, along with the commentary
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Graph Coloring",
        state = rememberWindowState(width = 1200.dp, height = 900.dp)
    ) {
        MaterialTheme {
            var maxVerticesText by remember { mutableStateOf("10") }
            var maxDegreeText by remember { mutableStateOf("3") }
            var stepsText by remember { mutableStateOf("10") }
            var graph by remember { mutableStateOf<Graph?>(null) }
            var canvasSize by remember { mutableStateOf(IntSize.Zero) }
            var layoutState by remember { mutableStateOf<LayoutState?>(null) }
            var running by remember { mutableStateOf(false) }

            LaunchedEffect(running) {
                if (!running) return@LaunchedEffect
                while (true) {
                    delay(200L)
                    val g = graph ?: break
                    val state = layoutState ?: break
                    val steps = stepsText.toIntOrNull()?.takeIf { it > 0 } ?: break
                    val w = canvasSize.width.toFloat()
                    val h = canvasSize.height.toFloat()
                    if (w <= 0 || h <= 0) break
                    val newState = GraphLayout.layoutIncrementally(state.positions, state.temperature, steps, g, w, h)
                    layoutState = newState
                    if (newState.temperature < 0.5f) break
                }
                running = false
            }

            // Initialize positions if canvasSize wasn't ready when graph was generated
            LaunchedEffect(canvasSize) {
                val g = graph ?: return@LaunchedEffect
                if (layoutState != null) return@LaunchedEffect
                if (canvasSize.width == 0 || canvasSize.height == 0) return@LaunchedEffect
                val w = canvasSize.width.toFloat()
                val h = canvasSize.height.toFloat()
                layoutState = GraphLayout.layoutIncrementally(null, GraphLayout.initialTemperature(w, h), 0, g, w, h)
            }

            val textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)

            Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Row 1: graph generation controls
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = maxVerticesText,
                        onValueChange = { if (it.all(Char::isDigit)) maxVerticesText = it },
                        label = { Text("Max Vertices", fontSize = 12.sp) },
                        textStyle = textStyle,
                        modifier = Modifier.width(120.dp),
                        singleLine = true
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedTextField(
                            value = maxDegreeText,
                            onValueChange = { if (it.all(Char::isDigit)) maxDegreeText = it },
                            label = { Text("Max Degree (graph)", fontSize = 12.sp) },
                            textStyle = textStyle,
                            modifier = Modifier.width(110.dp),
                            singleLine = true
                        )
                    }

                    Button(
                        onClick = {
                            val maxV = maxVerticesText.toIntOrNull() ?: return@Button
                            val maxD = maxDegreeText.toIntOrNull() ?: return@Button
                            if (maxV >= 2 && maxD >= 1) {
                                running = false
                                val newGraph = generateRandomGraph(maxV, maxD)
                                graph = newGraph
                                val w = canvasSize.width.toFloat()
                                val h = canvasSize.height.toFloat()
                                layoutState = if (w > 0 && h > 0) {
                                    GraphLayout.layoutIncrementally(null, GraphLayout.initialTemperature(w, h), 0, newGraph, w, h)
                                } else null
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) { Text("Generate graph", fontSize = 13.sp) }

                    Button(
                        onClick = {
                            val maxV = maxVerticesText.toIntOrNull() ?: return@Button
                            if (maxV >= 2) {
                                running = false
                                val newGraph = generateRandomBinaryTree(maxV)
                                graph = newGraph
                                val w = canvasSize.width.toFloat()
                                val h = canvasSize.height.toFloat()
                                layoutState = if (w > 0 && h > 0) {
                                    GraphLayout.layoutIncrementally(null, GraphLayout.initialTemperature(w, h), 0, newGraph, w, h)
                                } else null
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) { Text("Generate tree", fontSize = 13.sp) }
                }

                // Row 2: layout controls
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stepsText,
                        onValueChange = { if (it.all(Char::isDigit)) stepsText = it },
                        label = { Text("Steps", fontSize = 12.sp) },
                        textStyle = textStyle,
                        modifier = Modifier.width(90.dp),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            val g = graph ?: return@Button
                            val steps = stepsText.toIntOrNull() ?: return@Button
                            val state = layoutState ?: return@Button
                            val w = canvasSize.width.toFloat()
                            val h = canvasSize.height.toFloat()
                            if (steps > 0 && w > 0 && h > 0) {
                                layoutState = GraphLayout.layoutIncrementally(
                                    state.positions, state.temperature, steps, g, w, h
                                )
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) { Text("Next", fontSize = 13.sp) }

                    Button(
                        onClick = { running = !running },
                        enabled = graph != null,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) { Text(if (running) "Stop" else "Layout", fontSize = 13.sp) }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color.LightGray)
                        .onSizeChanged { canvasSize = it },
                    contentAlignment = Alignment.Center
                ) {
                    val currentGraph = graph
                    val positions = layoutState?.positions
                    if (currentGraph == null) {
                        Text(
                            "Click \"Generate Next\" to create a random graph",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    } else {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (positions == null || positions.size != currentGraph.vertexCount) return@Canvas

                            // Draw edges
                            for (edge in currentGraph.edges) {
                                val from = positions[edge.u]
                                val to = positions[edge.v]
                                drawLine(
                                    color = Color.DarkGray,
                                    start = Offset(from.x, from.y),
                                    end = Offset(to.x, to.y),
                                    strokeWidth = 2f
                                )
                            }

                            // Draw vertices
                            val radius = 14f
                            for (i in 0 until currentGraph.vertexCount) {
                                val pos = positions[i]
                                drawCircle(
                                    color = vertexColors[i % vertexColors.size],
                                    radius = radius,
                                    center = Offset(pos.x, pos.y)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
