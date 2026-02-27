package org.qwwdfsad

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

data class VertexPosition(val x: Float, val y: Float)

data class LayoutState(val positions: List<VertexPosition>, val temperature: Float)

/**
 * Fruchterman-Reingold particle simulation for graph layout
 *
 * Core idea:
 * - All vertices are particles
 * - Vertices are repelled from each other proportionally to k/dist^2
 * - Connected vertices are attracted to each other like dist^2/k
 *      - Mental model: gravity and rubber band (gosh, I have to come up with a better one, esp. taking Hooke law is linear)
 * - Temperature that basically controls convergence -- everything starts how
 *     and quickly moving, then gradually cools down iteration by iteration.
 *     Basically a thing from simulated annealing.
 *     TODO: make it dependent on the current state quality maybe
 *        Ideas:
 *            - Optimize ideal bounding
 *            - Displacement-based cooling (e.g. sum of dx dy)
 *            - Any energy-based cooling (sum of forces)
 * TODO:
 * - Play with k (currently -- k^2 ~ area/v^2 which is kinda evened-out distance)
 * - Basically scale/density of the simulation
 *    - High values will put it around the borders
 *    - Small values will just center everything
 * - Physical interpretation:
 *    - At what distance repulsion and attraction cancel out
 *
 * Rinse and repeat.
 */
object GraphLayout {

    fun initialTemperature(width: Float, height: Float): Float = min(width - 80f, height - 80f) / 2f

    fun layoutIncrementally(
        previousPositions: List<VertexPosition>?, temperature: Float, steps: Int,
        graph: Graph, width: Float, height: Float
    ): LayoutState {
        val n = graph.vertexCount
        if (n == 0) return LayoutState(emptyList(), temperature)
        if (n == 1) return LayoutState(listOf(VertexPosition(width / 2, height / 2)), temperature)

        val padding = 40f
        val w = width - 2 * padding
        val h = height - 2 * padding
        val area = w * h

        val random = Random(graph.hashCode())
        val posX = previousPositions?.map { it.x }?.toFloatArray() ?: FloatArray(n) { padding + random.nextFloat() * w }
        val posY = previousPositions?.map { it.y }?.toFloatArray() ?: FloatArray(n) { padding + random.nextFloat() * h }

        val k = sqrt(area / n.toFloat()) * graph.density()
        var t = temperature

        val dispX = FloatArray(n)
        val dispY = FloatArray(n)

        for (iter in 0 until steps) {
            dispX.fill(0f)
            dispY.fill(0f)
            // Repell
            for (i in 0 until n) {
                for (j in i + 1 until n) {
                    val dx = posX[i] - posX[j]
                    val dy = posY[i] - posY[j]
                    val dist = max(sqrt(dx * dx + dy * dy), 0.01f)
                    val force = k * k / dist
                    val fx = dx / dist * force
                    val fy = dy / dist * force
                    dispX[i] += fx
                    dispY[i] += fy
                    dispX[j] -= fx
                    dispY[j] -= fy
                }
            }
            // Attract
            for (edge in graph.edges) {
                val dx = posX[edge.u] - posX[edge.v]
                val dy = posY[edge.u] - posY[edge.v]
                val dist = max(sqrt(dx * dx + dy * dy), 0.01f)
                val force = dist * dist / k
                val fx = dx / dist * force
                val fy = dy / dist * force
                dispX[edge.u] -= fx
                dispY[edge.u] -= fy
                dispX[edge.v] += fx
                dispY[edge.v] += fy
            }
            // Temperature-based clamping
            for (i in 0 until n) {
                val dist = max(sqrt(dispX[i] * dispX[i] + dispY[i] * dispY[i]), 0.01f)
                val clamp = min(dist, t) / dist
                posX[i] += dispX[i] * clamp
                posY[i] += dispY[i] * clamp
                // Clamp within bounds
                posX[i] = posX[i].coerceIn(padding, width - padding)
                posY[i] = posY[i].coerceIn(padding, height - padding)
            }
            // Cooling down
            t *= 0.95f
        }

        return LayoutState((0 until n).map { VertexPosition(posX[it], posY[it]) }, t)
    }
}
