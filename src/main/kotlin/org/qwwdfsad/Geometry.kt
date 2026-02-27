package org.qwwdfsad

import kotlin.math.sqrt

fun findVertexByCoordinates(tapX: Float, tapY: Float, positions: List<VertexPosition>, radius: Float = 20f): Int? {
    var bestIndex: Int? = null
    var bestDist = Float.MAX_VALUE
    for (i in positions.indices) {
        val dx = tapX - positions[i].x;
        val dy = tapY - positions[i].y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist <= radius && dist < bestDist) {
            bestDist = dist; bestIndex = i
        }
    }
    return bestIndex
}

fun findEdgeByCoordinates(tapX: Float, tapY: Float, edges: Set<Edge>, positions: List<VertexPosition>, threshold: Float = 8f): Edge? {
    var bestEdge: Edge? = null
    var bestDist = Float.MAX_VALUE
    for (edge in edges) {
        val a = positions[edge.u];
        val b = positions[edge.v]
        val dist = pointToSegmentDist(tapX, tapY, a.x, a.y, b.x, b.y)
        if (dist <= threshold && dist < bestDist) {
            bestDist = dist; bestEdge = edge
        }
    }
    return bestEdge
}

private fun pointToSegmentDist(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
    val abx = bx - ax;
    val aby = by - ay
    val lenSq = abx * abx + aby * aby
    if (lenSq == 0f) return sqrt((px - ax) * (px - ax) + (py - ay) * (py - ay))
    val t = (((px - ax) * abx + (py - ay) * aby) / lenSq).coerceIn(0f, 1f)
    val cx = ax + t * abx;
    val cy = ay + t * aby
    return sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy))
}
