package com.frientrip.calendarview

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date

class DayView @JvmOverloads constructor(ctx: Context, attr: AttributeSet? = null, def: Int = 0):
        TextView(ctx, attr, def) {

    private var decorators: List<DayDecorator>? = null
    var date: Date? = null
        private set

    fun bind(date: Date, decorators: List<DayDecorator>) {
        this.date = date
        this.decorators = decorators
        text = Integer.parseInt(SimpleDateFormat("d").format(date)).toString()
    }

    fun decorate() {
        if (decorators != null) {
            val _decorators = (decorators as List<DayDecorator>)
            for (decorator in _decorators) decorator.decorate(this)
        }
    }
}