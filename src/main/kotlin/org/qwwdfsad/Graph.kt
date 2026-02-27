package org.qwwdfsad

import kotlin.random.Random

data class Edge(val u: Int, val v: Int)

data class Graph(val vertexCount: Int, val edges: Set<Edge>) {

    private val adjacency: Map<Int, List<Int>> = run {
        val map = mutableMapOf<Int, MutableList<Int>>()
        for (v in 0 until vertexCount) {
            map[v] = mutableListOf()
        }
        for (edge in edges) {
            map.getOrPut(edge.u) { mutableListOf() }.add(edge.v)
            map.getOrPut(edge.v) { mutableListOf() }.add(edge.u)
        }
        map
    }

    // 0..1
    // TODO: figure what to do with star topologies
    fun density() = (edges.size.toDouble() / (vertexCount * (vertexCount - 1) / 2)).toFloat()
}

fun generateRandomBinaryTree(maxVertices: Int): Graph {
    val vertexCount = maxVertices
    val edges = mutableSetOf<Edge>()
    val childCount = IntArray(vertexCount)

    val available = ArrayDeque<Int>()
    available.add(0) // root

    for (v in 1 until vertexCount) {
        val parentIdx = Random.nextInt(available.size)
        val parent = available[parentIdx]
        edges.add(Edge(parent, v))
        childCount[parent]++
        if (childCount[parent] >= 2) available.removeAt(parentIdx)
        available.add(v)
    }

    return Graph(vertexCount, edges)
}

/**
 * Algorithm
 * - Generate [2, max] vertices
 * - Try to add an edge 2*V^2 times, terminate the algo if it won't
 *
 * How complete the resulting graph is?
 *
 * - For a setup (mV, mD) there are F total possible edges. f is how many edges are left at current iteration
 * - On any attempt, probability of the success is ~2f/V^2
 * - Probability of algo termination is (1 - 2f/V^2)^(2V^2)
 *     - lim(1 + x/n)^2 ~ e^x
 *     - e^(-4f)
 * - In half of the cases, algo will terminate around f ~ 0.17.
 * Ughm, probably overthought it
 */
fun generateRandomGraph(maxVertices: Int, maxDegree: Int): Graph {
    val vertexCount = maxVertices
    val edges = mutableSetOf<Edge>()
    val degrees = IntArray(vertexCount)

    val maxAttempts = vertexCount * vertexCount * 2
    var attempts = 0

    while (attempts < maxAttempts) {
        val u = Random.nextInt(vertexCount)
        val v = Random.nextInt(vertexCount)
        if (u == v) {
            attempts++
            continue
        }

        val edge = if (u < v) Edge(u, v) else Edge(v, u)
        if (edge in edges) {
            attempts++
            continue
        }

        if (degrees[u] >= maxDegree || degrees[v] >= maxDegree) {
            attempts++
            continue
        }

        edges.add(edge)
        degrees[u]++
        degrees[v]++
        attempts = 0
    }

    return Graph(vertexCount, edges)
}
