package dev.definitelybenny.hexflipper.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import dev.definitelybenny.hexflipper.model.BoardState
import dev.definitelybenny.hexflipper.model.CellType
import dev.definitelybenny.hexflipper.model.HexCell
import dev.definitelybenny.hexflipper.model.HexStack
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// ---------------------------------------------------------------------------
// Animation types
// ---------------------------------------------------------------------------

enum class AnimationType { SLIDE_OFF, SHAKE }

data class AnimatingStack(
    val cell: HexCell,
    val stack: HexStack,
    val path: List<HexCell>,
    val type: AnimationType
)


// ---------------------------------------------------------------------------
// Visual constants
// ---------------------------------------------------------------------------

/** Dark board background. */
private val BackgroundColor = Color(0xFF0F0C29)

/** Empty cell fill — subtle on dark. */
private val EmptyCellFill = Color(0xFF2A2545)

/** Empty cell outline. */
private val EmptyCellOutline = Color(0xFF3D3660)

/** Wall cell fill. */
private val WallCellFill = Color(0xFF0A0818)

/** Depth edge thickness per stack tile, as fraction of hex size. */
private const val DEPTH_PER_TILE = 0.12f

// ---------------------------------------------------------------------------
// HexBoard composable
// ---------------------------------------------------------------------------

@Composable
fun HexBoard(
    boardState: BoardState,
    animatingStack: AnimatingStack?,
    onCellTapped: (HexCell) -> Unit,
    onAnimationFinished: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val animProgress = remember(animatingStack) { Animatable(0f) }

    LaunchedEffect(animatingStack) {
        if (animatingStack != null) {
            animProgress.snapTo(0f)
            val duration = when (animatingStack.type) {
                AnimationType.SLIDE_OFF -> {
                    // Scale duration to path length so each hop takes ~250ms
                    val segments = (animatingStack.path.size - 1).coerceAtLeast(1)
                    250 * segments
                }
                AnimationType.SHAKE -> 200
            }
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = duration, easing = LinearEasing)
            )
            onAnimationFinished()
        }
    }

    // Overload: accept the old parameter name so GameScreen compiles.
    @Suppress("NOTHING_TO_INLINE")
    HexBoardCanvas(boardState, animatingStack, animProgress, onCellTapped, modifier)
}


@Composable
private fun HexBoardCanvas(
    boardState: BoardState,
    animatingStack: AnimatingStack?,
    animProgress: Animatable<Float, *>,
    onCellTapped: (HexCell) -> Unit,
    modifier: Modifier
) {
    Canvas(
        modifier = modifier
            .pointerInput(boardState) {
                detectTapGestures { tapOffset ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val layout = computeLayout(boardState, w, h)
                    val tapped = hitTest(tapOffset.x, tapOffset.y, boardState, layout)
                    if (tapped != null) {
                        onCellTapped(tapped)
                    }
                }
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Light background
        drawRect(color = BackgroundColor)

        val layout = computeLayout(boardState, canvasWidth, canvasHeight)
        val hexSize = layout.hexSize

        val animCell = animatingStack?.cell

        // All board cells sorted by r ascending (back rows first) so depth edges overlap correctly.
        val allCells = boardState.boardCells.sortedWith(
            compareBy<HexCell> { it.r }.thenBy { it.q }
        )

        // --- Draw all cells ---
        for (cell in allCells) {
            val (cx, cy) = cellToScreen(cell, layout)
            val stack = boardState.stackAt(cell)

            when {
                // Cell has a stack (and is not the animating cell)
                stack != null && cell != animCell -> {
                    drawStackCell(cx, cy, hexSize, stack)
                }
                // Cell has a stack that is animating — draw the stack statically
                // (the animation overlay handles the moving piece)
                stack != null && cell == animCell -> {
                    // For SHAKE, draw the base (empty cell underneath) —
                    // the stack itself is drawn in the animation pass.
                    // For SLIDE_OFF, the stack is sliding away so show empty underneath.
                    drawEmptyCell(cx, cy, hexSize)
                }
                // Wall cell
                boardState.cells[cell] == CellType.WALL -> {
                    drawWallCell(cx, cy, hexSize)
                }
                // Empty cell
                else -> {
                    drawEmptyCell(cx, cy, hexSize)
                }
            }
        }

        // --- Draw animating stack ---
        if (animatingStack != null) {
            val t = animProgress.value
            val stack = animatingStack.stack

            when (animatingStack.type) {
                AnimationType.SLIDE_OFF -> {
                    val path = animatingStack.path
                    if (path.size >= 2) {
                        val totalSegments = path.size - 1
                        val rawIndex = t * totalSegments
                        val segIndex = rawIndex.toInt().coerceAtMost(totalSegments - 1)
                        val segT = rawIndex - segIndex

                        val from = cellToScreen(path[segIndex], layout)
                        val to = cellToScreen(
                            path[(segIndex + 1).coerceAtMost(path.lastIndex)],
                            layout
                        )

                        // Flip angle for this hop: 0 → PI (one full end-over-end)
                        val flipAngle = segT * Math.PI.toFloat()

                        // Position: linear between cells + semicircular hop arc
                        val linearX = from.first + (to.first - from.first) * segT
                        val linearY = from.second + (to.second - from.second) * segT
                        val hopHeight = hexSize * 0.4f
                        val arcLift = sin(flipAngle) * hopHeight
                        val cx = linearX
                        val cy = linearY - arcLift

                        // Flip squish: 1 → 0 → -1 (edge-on at midpoint)
                        val flipScale = cos(flipAngle)
                        val absScale = abs(flipScale).coerceAtLeast(0.05f)
                        val isBackFace = flipScale < 0f

                        // Fade out only on the final segment (off the board)
                        val isLastSeg = segIndex == totalSegments - 1
                        val alpha = if (isLastSeg) (1f - segT).coerceIn(0f, 1f) else 1f

                        drawFlippingStack(
                            cx, cy, hexSize, stack, alpha,
                            flipScale = absScale,
                            flipAxis = stack.direction.angleRadians,
                            showBack = isBackFace
                        )
                    }
                }

                AnimationType.SHAKE -> {
                    val (baseCx, baseCy) = cellToScreen(animatingStack.cell, layout)
                    val perpAngle = stack.direction.angleRadians + (Math.PI.toFloat() / 2f)
                    val shakeAmount = hexSize * 0.10f *
                        sin(t * 4f * Math.PI.toFloat()) * (1f - t)
                    val cx = baseCx + cos(perpAngle) * shakeAmount
                    val cy = baseCy + sin(perpAngle) * shakeAmount

                    drawStackCell(cx, cy, hexSize, stack, 1f)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Layout
// ---------------------------------------------------------------------------

private data class HexLayout(val hexSize: Float, val offsetX: Float, val offsetY: Float)

/**
 * Convert axial hex (q, r) to screen position (pointy-top).
 *
 * Pointy-top hex to pixel:
 *   px = size * (sqrt(3) * q + sqrt(3)/2 * r)
 *   py = size * (3/2 * r)
 */
private fun cellToScreen(cell: HexCell, layout: HexLayout): Pair<Float, Float> {
    val s = layout.hexSize
    val sqrt3 = sqrt(3f)
    val px = s * (sqrt3 * cell.q + sqrt3 / 2f * cell.r)
    val py = s * (1.5f * cell.r)
    return Pair(px + layout.offsetX, py + layout.offsetY)
}

/**
 * Compute hex size and offset so the board fits the canvas with padding.
 * Accounts for depth edges extending below the bottommost row.
 */
private fun computeLayout(
    boardState: BoardState,
    canvasWidth: Float,
    canvasHeight: Float
): HexLayout {
    val cells = boardState.boardCells
    if (cells.isEmpty()) return HexLayout(40f, canvasWidth / 2f, canvasHeight / 2f)

    val sqrt3 = sqrt(3f)
    var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
    var minY = Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
    var maxHeight = 1

    for (cell in cells) {
        val px = sqrt3 * cell.q + sqrt3 / 2f * cell.r
        val py = 1.5f * cell.r
        if (px < minX) minX = px
        if (px > maxX) maxX = px
        if (py < minY) minY = py
        if (py > maxY) maxY = py

        val stack = boardState.stackAt(cell)
        if (stack != null && stack.height > maxHeight) maxHeight = stack.height
    }

    // Add margin for hex radius on each side, plus bottom depth edge headroom
    val marginX = sqrt3
    val marginY = 2f
    val depthHeadroom = maxHeight * DEPTH_PER_TILE
    val unitWidth = (maxX - minX) + marginX * 2f
    val unitHeight = (maxY - minY) + marginY + depthHeadroom

    val padding = 32f
    val availW = canvasWidth - padding * 2f
    val availH = canvasHeight - padding * 2f

    val hexSize = min(availW / unitWidth, availH / unitHeight)

    val centerUnitX = (minX + maxX) / 2f
    val centerUnitY = (minY + maxY) / 2f + depthHeadroom * 0.3f
    val offsetX = canvasWidth / 2f - centerUnitX * hexSize
    val offsetY = canvasHeight / 2f - centerUnitY * hexSize

    return HexLayout(hexSize, offsetX, offsetY)
}

// ---------------------------------------------------------------------------
// Hit testing
// ---------------------------------------------------------------------------

/**
 * Hit-test tap position against stacks on the board.
 * Uses pointy-top hex math to convert pixel to axial, then checks for a stack.
 */
private fun hitTest(
    tapX: Float,
    tapY: Float,
    boardState: BoardState,
    layout: HexLayout
): HexCell? {
    // Convert tap to local hex-space coordinates
    val localX = tapX - layout.offsetX
    val localY = tapY - layout.offsetY
    val s = layout.hexSize

    val sqrt3 = sqrt(3f)
    val qf = (sqrt3 / 3f * localX - 1f / 3f * localY) / s
    val rf = (2f / 3f * localY) / s

    val cell = cubeRound(qf, rf)

    // Only return if there is a stack at this cell
    return if (boardState.stackAt(cell) != null) cell else null
}

private fun cubeRound(qf: Float, rf: Float): HexCell {
    val sf = -qf - rf
    var q = qf.roundToInt()
    var r = rf.roundToInt()
    val s = sf.roundToInt()

    val qDiff = abs(q - qf)
    val rDiff = abs(r - rf)
    val sDiff = abs(s - sf)

    if (qDiff > rDiff && qDiff > sDiff) {
        q = -r - s
    } else if (rDiff > sDiff) {
        r = -q - s
    }
    return HexCell(q, r)
}

// ---------------------------------------------------------------------------
// Hex vertex helpers (pointy-top)
// ---------------------------------------------------------------------------

/**
 * Pointy-top hex vertices. Vertex i at angle (60 * i - 30) degrees:
 *   0: upper-right, 1: right, 2: lower-right,
 *   3: lower-left, 4: left, 5: upper-left
 */
private fun hexVertices(cx: Float, cy: Float, size: Float): Array<Offset> {
    return Array(6) { i ->
        val angleDeg = 60f * i - 30f
        val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
        Offset(
            cx + size * cos(angleRad),
            cy + size * sin(angleRad)
        )
    }
}

private fun hexPath(vertices: Array<Offset>): Path {
    return Path().apply {
        moveTo(vertices[0].x, vertices[0].y)
        for (i in 1..5) lineTo(vertices[i].x, vertices[i].y)
        close()
    }
}

// ---------------------------------------------------------------------------
// Cell drawing
// ---------------------------------------------------------------------------

/** Draw an empty (gray) hex cell. */
private fun DrawScope.drawEmptyCell(cx: Float, cy: Float, hexSize: Float) {
    val r = hexSize * 0.90f
    val verts = hexVertices(cx, cy, r)
    val path = hexPath(verts)
    drawPath(path, color = EmptyCellFill, style = Fill)
    drawPath(path, color = EmptyCellOutline, style = Stroke(width = 1f))
}

/** Draw a wall (dark) hex cell. */
private fun DrawScope.drawWallCell(cx: Float, cy: Float, hexSize: Float) {
    val r = hexSize * 0.90f
    val verts = hexVertices(cx, cy, r)
    val path = hexPath(verts)
    drawPath(path, color = WallCellFill, style = Fill)
}

/**
 * Draw a colored stack cell with near-top-down 3D depth edge and white arrow.
 */
private fun DrawScope.drawStackCell(
    cx: Float,
    cy: Float,
    hexSize: Float,
    stack: HexStack,
    alpha: Float = 1f
) {
    val r = hexSize * 0.90f
    val topVerts = hexVertices(cx, cy, r)
    val depth = stack.height * hexSize * DEPTH_PER_TILE

    val mainColor = stack.color.start.copy(alpha = alpha)
    val darkColor = darkenColor(stack.color.start, 0.60f).copy(alpha = alpha)

    // --- Depth edge (side faces below the lower portion of the hex) ---
    // The lower vertices are 2 (lower-right) and 3 (lower-left),
    // with vertex 1 (right) and 4 (left) as adjacent.
    // Draw side quads: 1->2 shifted down, and 3->4 shifted down, plus bottom edge 2->3.
    if (depth > 0f) {
        // Bottom edge face: vertices 3 -> 2 (the bottom-most edge facing viewer)
        drawSideFace(
            topVerts[3], topVerts[2],
            Offset(topVerts[2].x, topVerts[2].y + depth),
            Offset(topVerts[3].x, topVerts[3].y + depth),
            darkColor
        )
        // Right side face: vertices 2 -> 1
        drawSideFace(
            topVerts[2], topVerts[1],
            Offset(topVerts[1].x, topVerts[1].y + depth),
            Offset(topVerts[2].x, topVerts[2].y + depth),
            darkenColor(stack.color.start, 0.50f).copy(alpha = alpha)
        )
        // Left side face: vertices 4 -> 3
        drawSideFace(
            topVerts[4], topVerts[3],
            Offset(topVerts[3].x, topVerts[3].y + depth),
            Offset(topVerts[4].x, topVerts[4].y + depth),
            darkenColor(stack.color.start, 0.50f).copy(alpha = alpha)
        )
    }

    // --- Top face (flat, solid color) ---
    val topPath = hexPath(topVerts)
    drawPath(topPath, color = mainColor, style = Fill)

    // --- White arrow ---
    drawArrow(cx, cy, hexSize * 0.88f, stack.direction.angleRadians, alpha)
}

/**
 * Draw a stack that is flipping end-over-end like a coin.
 * [flipScale] is 0..1 representing how "open" the face is (1 = full, ~0 = edge-on).
 * [flipAxis] is the movement direction angle; the hex squishes perpendicular to it.
 * [showBack] = true draws a darker "back face."
 */
private fun DrawScope.drawFlippingStack(
    cx: Float,
    cy: Float,
    hexSize: Float,
    stack: HexStack,
    alpha: Float,
    flipScale: Float,
    flipAxis: Float,
    showBack: Boolean
) {
    val r = hexSize * 0.90f
    // Squish along the movement direction (end-over-end tumble)
    val cosPerp = cos(flipAxis)
    val sinPerp = sin(flipAxis)

    // Generate vertices squished toward center along the perpendicular axis
    val verts = Array(6) { i ->
        val angleDeg = 60f * i - 30f
        val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
        val vx = r * cos(angleRad)
        val vy = r * sin(angleRad)

        // Project vertex onto perpendicular axis, then scale that component
        val dot = vx * cosPerp + vy * sinPerp
        val squished = dot * flipScale
        val newVx = vx + (squished - dot) * cosPerp
        val newVy = vy + (squished - dot) * sinPerp

        Offset(cx + newVx, cy + newVy)
    }

    val hexPath = hexPath(verts)

    val faceColor = if (showBack) {
        darkenColor(stack.color.start, 0.45f).copy(alpha = alpha)
    } else {
        stack.color.start.copy(alpha = alpha)
    }

    // Depth edge (only if enough scale to be visible)
    if (flipScale > 0.2f && !showBack) {
        val depth = stack.height * hexSize * DEPTH_PER_TILE * flipScale
        val darkColor = darkenColor(stack.color.start, 0.55f).copy(alpha = alpha)
        drawSideFace(
            verts[3], verts[2],
            Offset(verts[2].x, verts[2].y + depth),
            Offset(verts[3].x, verts[3].y + depth),
            darkColor
        )
    }

    drawPath(hexPath, color = faceColor, style = Fill)

    // Arrow only on front face when wide enough to see
    if (!showBack && flipScale > 0.3f) {
        drawArrow(cx, cy, hexSize * 0.88f * flipScale, stack.direction.angleRadians, alpha)
    }
}

/**
 * Draw a parallelogram side face for the depth edge.
 */
private fun DrawScope.drawSideFace(
    topLeft: Offset,
    topRight: Offset,
    bottomRight: Offset,
    bottomLeft: Offset,
    color: Color
) {
    val path = Path().apply {
        moveTo(topLeft.x, topLeft.y)
        lineTo(topRight.x, topRight.y)
        lineTo(bottomRight.x, bottomRight.y)
        lineTo(bottomLeft.x, bottomLeft.y)
        close()
    }
    drawPath(path, color = color, style = Fill)
}

/**
 * White direction arrow on the top face of a stack.
 */
private fun DrawScope.drawArrow(
    cx: Float,
    cy: Float,
    hexSize: Float,
    angleRad: Float,
    alpha: Float
) {
    val arrowLen = hexSize * 0.42f
    val arrowWidth = hexSize * 0.20f

    // Tip
    val tipX = cx + cos(angleRad) * arrowLen
    val tipY = cy + sin(angleRad) * arrowLen

    // Base center (slightly behind hex center)
    val baseCX = cx - cos(angleRad) * arrowLen * 0.25f
    val baseCY = cy - sin(angleRad) * arrowLen * 0.25f

    // Perpendicular for base width
    val perpAngle = angleRad + (Math.PI.toFloat() / 2f)
    val leftX = baseCX + cos(perpAngle) * arrowWidth
    val leftY = baseCY + sin(perpAngle) * arrowWidth
    val rightX = baseCX - cos(perpAngle) * arrowWidth
    val rightY = baseCY - sin(perpAngle) * arrowWidth

    val arrowPath = Path().apply {
        moveTo(tipX, tipY)
        lineTo(leftX, leftY)
        lineTo(rightX, rightY)
        close()
    }
    drawPath(
        arrowPath,
        color = Color.White.copy(alpha = 0.85f * alpha),
        style = Fill
    )
}

// ---------------------------------------------------------------------------
// Color utilities
// ---------------------------------------------------------------------------

/** Darken a color by multiplying RGB by [factor] (0 = black, 1 = unchanged). */
private fun darkenColor(color: Color, factor: Float): Color {
    return Color(
        red = (color.red * factor).coerceIn(0f, 1f),
        green = (color.green * factor).coerceIn(0f, 1f),
        blue = (color.blue * factor).coerceIn(0f, 1f),
        alpha = color.alpha
    )
}
