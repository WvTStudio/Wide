package com.wvt.codeeditor.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.Editable
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.core.view.marginEnd
import com.wvt.codeeditor.R

class EditableTextView(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ScrollableView(context, attrs, defStyleAttr) {
    var textSize: Float
    var textColor: Int
    var rowSpacing: Float
    var text: Editable
    var mTextPaint = Paint()

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.EditableTextView)
        textSize = typedArray.getDimension(
            R.styleable.EditableTextView_textSize,
            context.resources.getDimension(R.dimen.default_text_size)
        )
        textColor = typedArray.getColor(
            R.styleable.EditableTextView_textColor,
            ContextCompat.getColor(context, R.color.default_text_color)
        )
        rowSpacing = typedArray.getDimension(
            R.styleable.EditableTextView_rowSpacing,
            context.resources.getDimension(R.dimen.default_row_spacing)
        )
        text = Editable.Factory.getInstance().newEditable(
            typedArray.getString(R.styleable.EditableTextView_text) ?: ""
        )
        typedArray.recycle()
        resetStyle()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
    }

    private fun resetStyle() {
        mTextPaint.color = textColor
        mTextPaint.textSize = textSize
    }

    fun getRowHeight(): Float = textSize + rowSpacing
}