package com.zahit.keyview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.InputType
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import com.zahit.keyview.hidestrategy.HideNoneStrategy
import com.zahit.keyview.hidestrategy.HideStrategy
import com.zahit.keyview.listeners.KeyViewErrorListener
import com.zahit.keyview.listeners.KeyViewFinishedListener


/**
 * Created by zahit on 2019-10-07
 */
class KeyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle), View.OnKeyListener {

    private val textPaint = Paint()
    private val underlinePaint = Paint()
    private val debugRectanglePaint = Paint()

    private val charOuterDrawBounds = Rect()
    private val charInnerDrawBounds = Rect()
    private val charCenteredDrawBounds = Rect()

    // Set at runtime
    private val availableChars = mutableSetOf<Char>()
    private var maxCharWidth = 0
    private var maxCharHeight = 0
    private var layoutWidth = 0
    private var layoutHeight = 0

    private var previouslyHadError = false

    var textColor = Color.BLACK
        set(value) {
            field = value
            invalidate()
        }

    var textSize = 0f
        set(value) {
            field = value
            invalidate()
        }

    var keyToUnderlinePadding = 0
        set(value) {
            field = value
            invalidate()
        }


    var maxTextLength = 0
        set(value) {
            field = value
            currentText = ""
            invalidate()
        }

    var underlineColor = Color.BLACK
        set(value) {
            field = value
            invalidate()
        }

    var currentText = ""
        set(value) {
            field = value
            checkForErrors()
            checkForFinish()
            invalidate()
        }

    var hideCharacter = HideKey.DOT
        set(value) {
            field = value
            invalidate()
        }

    var hideStrategy: HideStrategy = HideNoneStrategy()
        set(value) {
            field = value
            invalidate()
        }

    var keyViewFinishedListener: KeyViewFinishedListener? = null
    var keyViewErrorListener: KeyViewErrorListener? = null
    var keyViewFilter: KeyViewFilter? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.key_view, this, true)
        setWillNotDraw(false)

        attrs?.let { attributeSet ->
            initAttrs(attributeSet)
        }

        initPaints()
        initCharDependentVariables()

        isFocusable = true
        isFocusableInTouchMode = true
        setOnKeyListener(this)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawText(canvas)
        drawUnderline(canvas)
    }

    private fun drawText(canvas: Canvas) {
        for (i in currentText.indices) {
            val currentCharAsString = if (hideStrategy.shouldHide(i, currentText.length)) {
                hideCharacter.charValue.toString()
            } else {
                currentText[i].toString()
            }

            val left = i * maxCharWidth
            val right = (i + 1) * maxCharWidth
            val top = 0
            val bottom = maxCharHeight

            charOuterDrawBounds.set(left, top, right, bottom)
            textPaint.getTextBounds(
                currentCharAsString,
                0,
                currentCharAsString.length,
                charInnerDrawBounds
            )

            centerRectangle(
                charInnerDrawBounds,
                charOuterDrawBounds,
                charCenteredDrawBounds
            )

            canvas.drawText(
                currentCharAsString,
                charCenteredDrawBounds.left.toFloat() + charCenteredDrawBounds.width() / 2f,
                charOuterDrawBounds.bottom.toFloat(),
                textPaint
            )
        }
    }

    private fun drawUnderline(canvas: Canvas) {
        for (i in currentText.length..maxTextLength) {
            val left = i * maxCharWidth + UNDERLINE_TO_UNDERLINE_MARGIN
            val right = (i + 1) * maxCharWidth - UNDERLINE_TO_UNDERLINE_MARGIN
            val top = maxCharHeight + keyToUnderlinePadding

            canvas.drawLine(
                left.toFloat(),
                top.toFloat(),
                right.toFloat(),
                top.toFloat(),
                underlinePaint
            )
        }
    }

    override fun onCheckIsTextEditor(): Boolean {
        return true
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection {
        val baseInputConnection = BaseInputConnection(this, false)
        outAttrs?.inputType = InputType.TYPE_NULL
        outAttrs?.imeOptions = EditorInfo.IME_ACTION_DONE
        return baseInputConnection
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(layoutWidth, layoutHeight)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            requestFocus()
            KeyViewUtil.showKeyboard(this)
        }

        return true
    }

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.action == KeyEvent.ACTION_DOWN) {
            val unicodeChar = event.unicodeChar.toChar()

            when {
                keyCode == KeyEvent.KEYCODE_DEL -> {
                    onDeleteClicked()
                    return true
                }
                keyCode == KeyEvent.KEYCODE_ENTER -> {
                    onEnterClicked()
                    return true
                }
                availableChars.contains(unicodeChar) -> {
                    onInputKeyClicked(unicodeChar)
                    return true
                }
            }
        }
        return false
    }

    private fun onInputKeyClicked(inputKey: Char) {
        if (currentText.length >= maxTextLength) {
            Log.d(TAG, "Attempt to write longer than maximum.")
        } else {
            if (keyViewFilter?.shouldFilter(inputKey) == true) {
                Log.d(TAG, "Key $inputKey has been filtered.")
            } else {
                currentText += inputKey
                checkForFinish()
            }
        }
    }

    private fun onDeleteClicked() {
        if (currentText.isEmpty()) {
            Log.d(TAG, "Attempt to delete when empty.")
        } else {
            currentText = currentText.substring(0, currentText.length - 1)
            Log.d(TAG, currentText)
        }
    }

    private fun onEnterClicked() {
        KeyViewUtil.hideKeyboard(this)
    }

    @Synchronized
    private fun checkForErrors() {
        keyViewErrorListener?.let { keyViewErrorListener ->
            val hasError = keyViewErrorListener.hasError(currentText)
            val errorStatusChanged = hasError != previouslyHadError

            if (errorStatusChanged) {
                keyViewErrorListener.onErrorStatusChanged(hasError, currentText)
                previouslyHadError = hasError
            }
        }
    }

    @Synchronized
    private fun checkForFinish() {
        if (currentText.length == maxTextLength) {
            keyViewFinishedListener?.onKeyFinished(currentText)
        }
    }

    private fun setLength(length: Int) {
        require(length > 0) { "Attribute key_view_length must be positive." }
        this.maxTextLength = length
    }

    private fun initAttrs(attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.KeyView, 0, 0
        )

        setLength(typedArray.getInt(R.styleable.KeyView_key_view_text_length, 1))

        textSize =
            typedArray.getDimensionPixelSize(
                R.styleable.KeyView_key_view_text_size,
                0
            ).toFloat()

        textColor = typedArray.getColor(
            R.styleable.KeyView_key_view_text_color,
            Color.BLACK
        )

        underlineColor = typedArray.getColor(
            R.styleable.KeyView_key_view_underline_color,
            Color.BLACK
        )

        hideCharacter = HideKey.values()[typedArray.getInt(
            R.styleable.KeyView_key_view_hide_character,
            0
        )]

        keyToUnderlinePadding =
            typedArray.getDimensionPixelSize(
                R.styleable.KeyView_key_view_key_to_underline_padding,
                0
            )

        typedArray.recycle()
    }

    private fun initCharDependentVariables() {
        availableChars.addAll((ASCII_PRINTABLE_CHAR_MIN..ASCII_PRINTABLE_CHAR_MAX).map { it.toChar() })

        val boundsRect = Rect()

        availableChars.forEach {
            val charAsStr = it.toString()
            textPaint.getTextBounds(charAsStr, 0, charAsStr.length, boundsRect)

            if (boundsRect.width() > maxCharWidth) {
                maxCharWidth = boundsRect.width()
            }

            if (boundsRect.height() > maxCharHeight) {
                maxCharHeight = boundsRect.height()
            }
        }

        layoutWidth = maxCharWidth * maxTextLength
        layoutHeight = maxCharHeight * 2
        layoutParams = LayoutParams(layoutWidth, layoutHeight)
    }

    private fun initPaints() {
        textPaint.textSize = this.textSize
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = textColor

        underlinePaint.color = underlineColor
        underlinePaint.isAntiAlias = true
        underlinePaint.isDither = true
        underlinePaint.style = Paint.Style.STROKE
        underlinePaint.strokeWidth = UNDERLINE_STOKE_WIDTH

        debugRectanglePaint.style = Paint.Style.STROKE
        debugRectanglePaint.strokeWidth = 2f
        debugRectanglePaint.color = Color.RED
    }

    private fun centerRectangle(
        innerRectangle: Rect,
        outerRectangle: Rect,
        resultingRectangle: Rect
    ) {
        val widthDiff = outerRectangle.width() - innerRectangle.width()
        val heightDiff = outerRectangle.height() - innerRectangle.height()
        resultingRectangle.set(
            outerRectangle.left + widthDiff / 2,
            outerRectangle.top + heightDiff / 2,
            outerRectangle.right - widthDiff / 2,
            outerRectangle.bottom - heightDiff / 2
        )
    }

    private fun drawDebugBounds(canvas: Canvas, rect: Rect) {
        if (DRAW_DEBUG_RECTANGLES) {
            canvas.drawRect(rect, debugRectanglePaint)
        }
    }

    companion object {
        private const val TAG = "KeyView"

        private const val ASCII_PRINTABLE_CHAR_MIN = 32
        private const val ASCII_PRINTABLE_CHAR_MAX = 126

        private const val UNDERLINE_TO_UNDERLINE_MARGIN = 10
        private const val UNDERLINE_STOKE_WIDTH = 5f

        private const val DRAW_DEBUG_RECTANGLES = true
    }
}