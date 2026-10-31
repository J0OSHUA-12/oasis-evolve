package com.oasisevolve.launcher.widgets

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.abs

/**
 * Detecta la pulsación larga sobre un widget antes de que le llegue el toque. Sin esto, mantener
 * pulsado un widget acabaría abriendo su app al soltar, en lugar de editar la pantalla.
 */
class EvolveWidgetHostView(context: Context) : AppWidgetHostView(context) {
    var onLongPress: (() -> Unit)? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var longPressed = false
    private val trigger = Runnable {
        longPressed = true
        performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        onLongPress?.invoke()
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                longPressed = false
                postDelayed(trigger, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE ->
                if (abs(event.x - downX) > touchSlop || abs(event.y - downY) > touchSlop) removeCallbacks(trigger)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> removeCallbacks(trigger)
        }
        // Tras la pulsación larga, el widget deja de recibir este toque y no se abre.
        return longPressed
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            removeCallbacks(trigger)
            longPressed = false
        }
        return longPressed || super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(trigger)
        super.onDetachedFromWindow()
    }
}
