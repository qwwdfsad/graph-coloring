package org.qwwdfsad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
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

private val controlTextStyle = TextStyle(fontSize = 13.sp)

// Note: all UI is mostly CC-generated, along with the commentary
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Graph Coloring",
        state = rememberWindowState(width = 1200.dp, height = 900.dp)
    ) {
        MaterialTheme { GraphColoringApp() }
    }
}

@Composable
private fun GraphColoringApp() {
    var maxVerticesText by remember { mutableStateOf("10") }
    var maxDegreeText by remember { mutableStateOf("3") }
    var stepsText by remember { mutableStateOf("10") }
    var graph by remember { mutableStateOf<Graph?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var layoutState by remember { mutableStateOf<LayoutState?>(null) }
    var running by remember { mutableStateOf(false) }
    var selectedVertex by remember { mutableStateOf<Int?>(null) }

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

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GenerationControlsRow(
            maxVerticesText = maxVerticesText,
            onMaxVerticesChange = { maxVerticesText = it },
            maxDegreeText = maxDegreeText,
            onMaxDegreeChange = { maxDegreeText = it },
            onGenerateGraph = { maxV, maxD ->
                running = false
                selectedVertex = null
                val newGraph = generateRandomGraph(maxV, maxD)
                graph = newGraph
                val w = canvasSize.width.toFloat()
                val h = canvasSize.height.toFloat()
                layoutState = if (w > 0 && h > 0)
                    GraphLayout.layoutIncrementally(null, GraphLayout.initialTemperature(w, h), 0, newGraph, w, h)
                else null
            },
            onGenerateTree = { maxV ->
                running = false
                selectedVertex = null
                val newGraph = generateRandomBinaryTree(maxV)
                graph = newGraph
                val w = canvasSize.width.toFloat()
                val h = canvasSize.height.toFloat()
                layoutState = if (w > 0 && h > 0) GraphLayout.layoutTree(newGraph, w, h) else null
            }
        )

        LayoutControlsRow(
            stepsText = stepsText,
            onStepsChange = { stepsText = it },
            running = running,
            graphReady = graph != null,
            onNext = {
                val g = graph
                val steps = stepsText.toIntOrNull()
                val state = layoutState
                val w = canvasSize.width.toFloat()
                val h = canvasSize.height.toFloat()
                if (g != null && steps != null && state != null && steps > 0 && w > 0 && h > 0) {
                    layoutState = GraphLayout.layoutIncrementally(state.positions, state.temperature, steps, g, w, h)
                }
            },
            onToggleLayout = {
                if (!running) {
                    val w = canvasSize.width.toFloat()
                    val h = canvasSize.height.toFloat()
                    if (w > 0 && h > 0) {
                        layoutState = layoutState?.copy(temperature = GraphLayout.initialTemperature(w, h))
                    }
                }
                running = !running
            }
        )

        GraphCanvasBox(
            graph = graph,
            positions = layoutState?.positions,
            selectedVertex = selectedVertex,
            onSizeChanged = { canvasSize = it },
            onVertexTap = { v ->
                val first = selectedVertex
                when {
                    first == null -> selectedVertex = v
                    first == v -> selectedVertex = null
                    else -> {
                        val g = graph!!
                        val u = minOf(first, v); val w = maxOf(first, v)
                        graph = g.copy(edges = g.edges + Edge(u, w))
                        selectedVertex = null
                    }
                }
            },
            onEdgeTap = { edge ->
                graph = graph!!.copy(edges = graph!!.edges - edge)
                selectedVertex = null
            },
            onEmptyTap = { selectedVertex = null }
        )
    }
}

@Composable
private fun GenerationControlsRow(
    maxVerticesText: String,
    onMaxVerticesChange: (String) -> Unit,
    maxDegreeText: String,
    onMaxDegreeChange: (String) -> Unit,
    onGenerateGraph: (maxV: Int, maxD: Int) -> Unit,
    onGenerateTree: (maxV: Int) -> Unit,
) {
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = maxVerticesText,
            onValueChange = { if (it.all(Char::isDigit)) onMaxVerticesChange(it) },
            label = { Text("Max Vertices", fontSize = 12.sp) },
            textStyle = controlTextStyle,
            modifier = Modifier.width(120.dp),
            singleLine = true
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedTextField(
                value = maxDegreeText,
                onValueChange = { if (it.all(Char::isDigit)) onMaxDegreeChange(it) },
                label = { Text("Max Degree (graph)", fontSize = 12.sp) },
                textStyle = controlTextStyle,
                modifier = Modifier.width(110.dp),
                singleLine = true
            )
        }

        Button(
            onClick = {
                val maxV = maxVerticesText.toIntOrNull() ?: return@Button
                val maxD = maxDegreeText.toIntOrNull() ?: return@Button
                if (maxV >= 2 && maxD >= 1) onGenerateGraph(maxV, maxD)
            },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            modifier = Modifier.align(Alignment.CenterVertically)
        ) { Text("Generate graph", fontSize = 13.sp) }

        Button(
            onClick = {
                val maxV = maxVerticesText.toIntOrNull() ?: return@Button
                if (maxV >= 2) onGenerateTree(maxV)
            },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            modifier = Modifier.align(Alignment.CenterVertically)
        ) { Text("Generate tree", fontSize = 13.sp) }
    }
}

@Composable
private fun LayoutControlsRow(
    stepsText: String,
    onStepsChange: (String) -> Unit,
    running: Boolean,
    graphReady: Boolean,
    onNext: () -> Unit,
    onToggleLayout: () -> Unit,
) {
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = stepsText,
            onValueChange = { if (it.all(Char::isDigit)) onStepsChange(it) },
            label = { Text("Steps", fontSize = 12.sp) },
            textStyle = controlTextStyle,
            modifier = Modifier.width(90.dp),
            singleLine = true
        )

        Button(
            onClick = onNext,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            modifier = Modifier.align(Alignment.CenterVertically)
        ) { Text("Next", fontSize = 13.sp) }

        Button(
            onClick = onToggleLayout,
            enabled = graphReady,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            modifier = Modifier.align(Alignment.CenterVertically)
        ) { Text(if (running) "Stop" else "Layout", fontSize = 13.sp) }
    }
}

@Composable
private fun GraphCanvasBox(
    graph: Graph?,
    positions: List<VertexPosition>?,
    selectedVertex: Int?,
    onSizeChanged: (IntSize) -> Unit,
    onVertexTap: (Int) -> Unit,
    onEdgeTap: (Edge) -> Unit,
    onEmptyTap: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, Color.LightGray)
            .onSizeChanged(onSizeChanged),
        contentAlignment = Alignment.Center
    ) {
        if (graph == null) {
            Text(
                "Click \"Generate Next\" to create a random graph",
                fontSize = 16.sp,
                color = Color.Gray
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize().pointerInput(graph, positions) {
                detectTapGestures { tapOffset ->
                    if (positions == null || positions.size != graph.vertexCount) return@detectTapGestures
                    val tappedVertex = findVertexByCoordinates(tapOffset.x, tapOffset.y, positions)
                    if (tappedVertex != null) {
                        onVertexTap(tappedVertex)
                        return@detectTapGestures
                    }
                    val tappedEdge = findEdgeByCoordinates(tapOffset.x, tapOffset.y, graph.edges, positions)
                    if (tappedEdge != null) {
                        onEdgeTap(tappedEdge)
                        return@detectTapGestures
                    }
                    onEmptyTap()
                }
            }) {
                if (positions == null || positions.size != graph.vertexCount) return@Canvas

                // Draw edges
                for (edge in graph.edges) {
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
                for (i in 0 until graph.vertexCount) {
                    val pos = positions[i]
                    if (i == selectedVertex) {
                        drawCircle(color = Color.White, radius = radius + 5f,
                            center = Offset(pos.x, pos.y), style = Stroke(width = 3f))
                    }
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
