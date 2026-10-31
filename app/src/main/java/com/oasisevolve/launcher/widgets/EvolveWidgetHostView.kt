package com.oasisevolve.launcher.widgets

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import kotlin.math.abs

/** Tiempo que se espera a que un widget envíe su contenido antes de decir que no lo ha hecho. */
private const val EMPTY_NOTICE_DELAY_MS = 5_000L

/**
 * Detecta la pulsación larga sobre un widget antes de que le llegue el toque. Sin esto, mantener
 * pulsado un widget acabaría abriendo su app al soltar, en lugar de editar la pantalla.
 *
 * También avisa si el widget nunca envía contenido, como los de otro launcher que solo se
 * actualizan dentro de él. Sin el aviso se vería un hueco vacío sin explicación.
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

    private var hasContent = false
    private var emptyNotice: TextView? = null
    private val showEmptyNotice = Runnable {
        if (!hasContent && emptyNotice == null) {
            emptyNotice = TextView(context).apply {
                text = "Este widget no envió contenido. Puede que solo funcione en el launcher o la app de su fabricante."
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.argb(196, 20, 22, 24))
                gravity = Gravity.CENTER
                val padding = (16 * resources.displayMetrics.density).toInt()
                setPadding(padding, padding, padding, padding)
            }.also { addView(it, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)) }
        }
    }

    override fun updateAppWidget(remoteViews: RemoteViews?) {
        super.updateAppWidget(remoteViews)
        if (remoteViews != null) {
            hasContent = true
            removeCallbacks(showEmptyNotice)
            emptyNotice?.let(::removeView)
            emptyNotice = null
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!hasContent) postDelayed(showEmptyNotice, EMPTY_NOTICE_DELAY_MS)
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
        removeCallbacks(showEmptyNotice)
        super.onDetachedFromWindow()
    }
}
