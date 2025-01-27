package merybina.example.pdfrendering.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.recyclerview.widget.RecyclerView

class PinchZoomRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle) {

    private var activePointerId = INVALID_POINTER_ID
    private val scaleDetector: ScaleGestureDetector by lazy {
        ScaleGestureDetector(context, ScaleListener())
    }
    private val gestureDetector: GestureDetector by lazy {
        GestureDetector(context, doubleTapListener)
    }
    private var currentScaleFactor = 1f
    private var maxWidth = 0.0f
    private var maxHeight = 0.0f
    private var lastTouchX: Float = 0.toFloat()
    private var lastTouchY: Float = 0.toFloat()
    private var positionX: Float = 0.toFloat()
    private var positionY: Float = 0.toFloat()


    var minZoom = 1.0f
    var maxZoom = 3.0f
    private val middleZoom get() = minZoom + (maxZoom - minZoom) / 2f

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        try {
            return super.onInterceptTouchEvent(ev)
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
        }

        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        super.onTouchEvent(event)

        when (val action = event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y
                lastTouchX = x
                lastTouchY = y
                activePointerId = event.getPointerId(0)
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerIndex =
                    action and MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                val dx = x - lastTouchX
                val dy = y - lastTouchY

                positionX += dx
                positionY += dy

                if (positionX > 0.0f) {
                    positionX = 0.0f
                } else if (positionX < maxWidth) {
                    positionX = maxWidth
                }

                if (positionY > 0.0f) {
                    positionY = 0.0f
                } else if (positionY < maxHeight) {
                    positionY = maxHeight
                }

                lastTouchX = x
                lastTouchY = y

                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                activePointerId = INVALID_POINTER_ID
            }

            MotionEvent.ACTION_CANCEL -> {
                activePointerId = INVALID_POINTER_ID
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex =
                    action and MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId == activePointerId) {
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    lastTouchX = event.getX(newPointerIndex)
                    lastTouchY = event.getY(newPointerIndex)
                    activePointerId = event.getPointerId(newPointerIndex)
                }
            }
        }

        return true
    }

    override fun fling(velocityX: Int, velocityY: Int): Boolean {
        val smoothingFactor = if (currentScaleFactor == 1f) 1f else currentScaleFactor * 2
        return super.fling(
            (velocityX / smoothingFactor).toInt(),
            (velocityY / smoothingFactor).toInt()
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.translate(positionX, positionY)
        canvas.scale(currentScaleFactor, currentScaleFactor)
        canvas.restore()
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.save()
        if (currentScaleFactor == minZoom) {
            positionX = 0.0f
            positionY = 0.0f
        }
        canvas.translate(positionX, positionY)
        canvas.scale(currentScaleFactor, currentScaleFactor)
        super.dispatchDraw(canvas)
        canvas.restore()
        invalidate()
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            adjustForNewScaleFactor(detector.scaleFactor, detector.focusX, detector.focusY)
            return true
        }
    }

    private val doubleTapListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(event: MotionEvent): Boolean {
            val newScaleFactor = if (currentScaleFactor < middleZoom) {
                middleZoom
            } else {
                minZoom
            }

            adjustForNewScaleFactor(newScaleFactor / currentScaleFactor, event.getX(), event.getY())
            return true
        }
    }

    private fun adjustForNewScaleFactor(newScaleFactorDiff: Float, eventX: Float, eventY: Float) {
        currentScaleFactor *= newScaleFactorDiff
        currentScaleFactor = currentScaleFactor.coerceIn(minZoom, maxZoom)

        if (currentScaleFactor < maxZoom) {
            val centerX = eventX
            val centerY = eventY
            var diffX = centerX - positionX
            var diffY = centerY - positionY
            diffX = diffX * newScaleFactorDiff - diffX
            diffY = diffY * newScaleFactorDiff - diffY
            positionX -= diffX
            positionY -= diffY
        }

        maxWidth = width - width * currentScaleFactor
        maxHeight = height - height * currentScaleFactor

        invalidate()
    }

    companion object {
        private const val INVALID_POINTER_ID = -1
    }
}