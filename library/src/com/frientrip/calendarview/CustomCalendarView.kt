package com.frientrip.calendarview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import java.text.DateFormatSymbols
import java.util.*

class CustomCalendarView @JvmOverloads constructor
    (private val ctx: Context, attrs: AttributeSet? = null): LinearLayout(ctx, attrs) {

    private var view: View? = null
    private var previousMonthButton: ImageView? = null
    private var nextMonthButton: ImageView? = null

    var dateListener: OnDateSelectListener? = null
    var monthListener: OnMonthChangedListener? = null
    var decorators: List<DayDecorator> = ArrayList()

    private var locale: Locale? = null
    private var lastSelectedDay: Date? = null

    var customTypeface: Typeface? = null
    var firstDayOfWeek = Calendar.SUNDAY

    var currentCalendar: Calendar? = null
        private set

    private var disabledDayBackgroundColor: Int = 0
    private var disabledDayTextColor: Int = 0
    private var calendarBackgroundColor: Int = 0
    private var selectedDayBackground: Int = 0
    private var weekLayoutBackgroundColor: Int = 0
    private var calendarTitleBackgroundColor: Int = 0
    private var selectedDayTextColor: Int = 0
    private var calendarTitleTextColor: Int = 0
    private var dayOfWeekTextColor: Int = 0
    private var dayOfMonthTextColor: Int = 0
    private var currentDayOfMonth: Int = 0
    private var weekendTextColor: Int = 0

    private var currentMonthIndex = 0
    var isOverflowDateVisible = true
        private set

    init {
        if (!isInEditMode) {
            if (attrs != null) getAttributes(attrs)
            initializeCalendar()
        }
    }

    private fun getAttributes(attrs: AttributeSet) {
        val array = ctx.obtainStyledAttributes(attrs, R.styleable.CustomCalendarView, 0, 0)
        val white = resources.getColor(R.color.white); val black = resources.getColor(R.color.black)

        calendarBackgroundColor = array.getColor(R.styleable.CustomCalendarView_calendarBackgroundColor, white)
        calendarTitleBackgroundColor = array.getColor(R.styleable.CustomCalendarView_titleLayoutBackgroundColor, white)
        calendarTitleTextColor = array.getColor(R.styleable.CustomCalendarView_calendarTitleTextColor, black)
        weekLayoutBackgroundColor = array.getColor(R.styleable.CustomCalendarView_weekLayoutBackgroundColor, white)
        dayOfWeekTextColor = array.getColor(R.styleable.CustomCalendarView_dayOfWeekTextColor, black)
        dayOfMonthTextColor = array.getColor(R.styleable.CustomCalendarView_dayOfMonthTextColor, black)
        disabledDayBackgroundColor = array.getColor(R.styleable.CustomCalendarView_disabledDayBackgroundColor,
                resources.getColor(R.color.day_disabled_background_color))
        disabledDayTextColor = array.getColor(R.styleable.CustomCalendarView_disabledDayTextColor,
                resources.getColor(R.color.day_disabled_text_color))
        selectedDayBackground = array.getColor(R.styleable.CustomCalendarView_selectedDayBackgroundColor,
                resources.getColor(R.color.selected_day_background))
        selectedDayTextColor = array.getColor(R.styleable.CustomCalendarView_selectedDayTextColor, white)
        currentDayOfMonth = array.getColor(R.styleable.CustomCalendarView_currentDayOfMonthColor,
                resources.getColor(R.color.current_day_of_month))
        weekendTextColor = array.getColor(R.styleable.CustomCalendarView_weekendTextColor, dayOfWeekTextColor)
        array.recycle()
    }

    private fun initializeCalendar() {
        val inflate = ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        view = inflate.inflate(R.layout.custom_calendar_layout, this, true)
        previousMonthButton = view!!.findViewById(R.id.leftButton) as ImageView
        nextMonthButton = view!!.findViewById(R.id.rightButton) as ImageView

        previousMonthButton!!.setOnClickListener {
            currentMonthIndex--
            currentCalendar = Calendar.getInstance(Locale.getDefault())
            currentCalendar!!.add(Calendar.MONTH, currentMonthIndex)
            refreshCalendar(currentCalendar as Calendar)
            if (monthListener != null) {
                (monthListener as OnMonthChangedListener).onMonthChanged(currentCalendar?.time)
            }
        }

        nextMonthButton!!.setOnClickListener {
            currentMonthIndex++
            currentCalendar = Calendar.getInstance(Locale.getDefault())
            currentCalendar!!.add(Calendar.MONTH, currentMonthIndex)
            refreshCalendar(currentCalendar as Calendar)
            if (monthListener != null) {
                (monthListener as OnMonthChangedListener).onMonthChanged(currentCalendar?.time)
            }
        }

        // Initialize calendar for current month
        val locale = ctx.resources.configuration.locale
        val currentCalendar = Calendar.getInstance(locale)

        firstDayOfWeek = Calendar.SUNDAY
        refreshCalendar(currentCalendar)
    }


    /**
     * Display calendar title with next previous month button
     */
    private fun initializeTitleLayout() {
        val titleLayout = view!!.findViewById(R.id.titleLayout)
        titleLayout.setBackgroundColor(calendarTitleBackgroundColor)

        var dateText = DateFormatSymbols(locale).shortMonths[currentCalendar!!.get(Calendar.MONTH)].toString()
        dateText = dateText.substring(0, 1).toUpperCase() + dateText.subSequence(1, dateText.length)

        val yearTitle = view!!.findViewById(R.id.yearTitle) as TextView
        yearTitle.text = currentCalendar!!.get(Calendar.YEAR).toString()

        val dateTitle = view!!.findViewById(R.id.dateTitle) as TextView
        dateTitle.setTextColor(calendarTitleTextColor)
        dateTitle.text = dateText
        dateTitle.setTextColor(calendarTitleTextColor)
        if (null != customTypeface) {
            dateTitle.setTypeface(customTypeface, Typeface.BOLD)
        }

    }

    /**
     * Initialize the calendar week layout, considers start day
     */
    @SuppressLint("DefaultLocale")
    private fun initializeWeekLayout() {
        var dayOfWeek: TextView
        var dayOfTheWeekString: String

        //Setting background color white
        val titleLayout = view!!.findViewById(R.id.weekLayout)
        titleLayout.setBackgroundColor(weekLayoutBackgroundColor)

        val weekDaysArray = DateFormatSymbols(locale).shortWeekdays
        for (i in 1..weekDaysArray.size - 1) {
            dayOfTheWeekString = weekDaysArray[i]
            if (dayOfTheWeekString.length > 3) {
                dayOfTheWeekString = dayOfTheWeekString.substring(0, 3).toUpperCase()
            }

            dayOfWeek = view!!.findViewWithTag(DAY_OF_WEEK + getWeekIndex(i, currentCalendar)) as TextView
            dayOfWeek.text = dayOfTheWeekString
            dayOfWeek.setTextColor(dayOfWeekTextColor)

            if (null != customTypeface) {
                dayOfWeek.typeface = customTypeface
            }
        }
    }

    private fun setDaysInCalendar() {
        val calendar = Calendar.getInstance(locale)
        calendar.time = currentCalendar!!.time
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.firstDayOfWeek = firstDayOfWeek
        val firstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK)

        // Calculate dayOfMonthIndex
        var dayOfMonthIndex = getWeekIndex(firstDayOfMonth, calendar)
        val actualMaximum = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val startCalendar = calendar.clone() as Calendar
        //Add required number of days
        startCalendar.add(Calendar.DATE, -(dayOfMonthIndex - 1))
        val monthEndIndex = 42 - (actualMaximum + dayOfMonthIndex - 1)

        var dayView: DayView
        var dayOfMonthContainer: ViewGroup
        for (i in 1..42) {
            dayOfMonthContainer = view!!.findViewWithTag(DAY_OF_MONTH_CONTAINER + i) as ViewGroup
            dayView = view?.findViewWithTag(DAY_OF_MONTH_TEXT + i) as DayView

            //Apply the default styles
            dayOfMonthContainer.setOnClickListener(null)
            dayView.bind(startCalendar.time, decorators)
            dayView.visibility = View.VISIBLE

            if (null != customTypeface) {
                dayView.typeface = customTypeface
            }

            if (isSameMonth(calendar, startCalendar)) {
                dayOfMonthContainer.setOnClickListener(onDayOfMonthClickListener)
                dayView.setBackgroundColor(calendarBackgroundColor)

                if (startCalendar
                        .get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                    dayView.setTextColor(weekendTextColor)
                else dayView.setTextColor(dayOfWeekTextColor)

                //Set the current day color
                markDayAsCurrentDay(startCalendar)
            } else {
                dayView.setBackgroundColor(disabledDayBackgroundColor)
                dayView.setTextColor(disabledDayTextColor)

                if (!isOverflowDateVisible)
                    dayView.visibility = View.GONE
                else if (i >= 36 && monthEndIndex.toFloat() / 7.0f >= 1) {
                    dayView.visibility = View.GONE
                }
            }
            dayView.decorate()



            startCalendar.add(Calendar.DATE, 1)
            dayOfMonthIndex++
        }

        // If the last week row has no visible days, hide it or show it in case
        val weekRow = view!!.findViewWithTag("weekRow6") as ViewGroup
        dayView = view!!.findViewWithTag("dayOfMonthText36") as DayView
        if (dayView.visibility != View.VISIBLE) {
            weekRow.visibility = View.GONE
        } else {
            weekRow.visibility = View.VISIBLE
        }
    }


    fun isSameMonth(c1: Calendar, c2: Calendar): Boolean {
        return c1.get(Calendar.ERA) == c2.get(Calendar.ERA)
                && c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
    }


    private fun clearDayOfTheMonthStyle(currentDate: Date) {
        val calendar = todaysCalendar
        calendar.firstDayOfWeek = firstDayOfWeek
        calendar.time = currentDate

        val dayView = getDayOfMonthText(calendar)
        dayView.setBackgroundColor(calendarBackgroundColor)
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            dayView.setTextColor(weekendTextColor)
        else dayView.setTextColor(dayOfWeekTextColor)
        dayView.decorate()
    }

    private fun getDayOfMonthText(currentCalendar: Calendar): DayView {
        return getView(DAY_OF_MONTH_TEXT, currentCalendar) as DayView
    }

    private fun getDayIndexByDate(currentCalendar: Calendar): Int {
        val monthOffset = getMonthOffset(currentCalendar)
        val currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH)
        val index = currentDay + monthOffset
        return index
    }

    private fun getMonthOffset(currentCalendar: Calendar): Int {
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = firstDayOfWeek
        calendar.time = currentCalendar.time
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayWeekPosition = calendar.firstDayOfWeek
        val dayPosition = calendar.get(Calendar.DAY_OF_WEEK)

        if (firstDayWeekPosition == 1)
            return dayPosition - 1
        else if (dayPosition == 1) return 6
        else return dayPosition - 2
    }

    private fun getWeekIndex(weekIndex: Int, currentCalendar: Calendar?): Int {
        val firstDayWeekPosition = currentCalendar?.firstDayOfWeek

        if (firstDayWeekPosition == 1) return weekIndex
        else if (weekIndex == 1) return 7
        else return weekIndex - 1
    }

    private fun getView(key: String, currentCalendar: Calendar): View {
        val index = getDayIndexByDate(currentCalendar)
        val childView = view!!.findViewWithTag(key + index)
        return childView
    }

    private val todaysCalendar: Calendar
        get() {
            val currentCalendar = Calendar.getInstance(ctx.resources.configuration.locale)
            currentCalendar.firstDayOfWeek = firstDayOfWeek
            return currentCalendar
        }

    @SuppressLint("DefaultLocale")
    fun refreshCalendar(currentCalendar: Calendar) {
        this.currentCalendar = currentCalendar
        this.currentCalendar!!.firstDayOfWeek = firstDayOfWeek
        locale = ctx.resources.configuration.locale

        // Set date title
        initializeTitleLayout()

        // Set weeks days titles
        initializeWeekLayout()

        // Initialize and set days in calendar
        setDaysInCalendar()
    }

    fun markDayAsCurrentDay(calendar: Calendar?) {
        if (calendar != null && isToday(calendar)) {
            val dayOfMonth = getDayOfMonthText(calendar)
            dayOfMonth.setTextColor(currentDayOfMonth)
        }
    }

    fun markDayAsSelectedDay(currentDate: Date) {
        val currentCalendar = todaysCalendar
        currentCalendar.firstDayOfWeek = firstDayOfWeek
        currentCalendar.time = currentDate

        // Clear previous marks
        if (lastSelectedDay != null) {
            val _lastSelectedDay = lastSelectedDay as Date
            clearDayOfTheMonthStyle(_lastSelectedDay)
        }

        // Store current values as last values
        storeLastValues(currentDate)

        // Mark current day as selected
        val view = getDayOfMonthText(currentCalendar)
        view.setBackgroundColor(selectedDayBackground)
        view.setTextColor(selectedDayTextColor)
    }

    private fun storeLastValues(currentDate: Date) {
        lastSelectedDay = currentDate
    }

    private val onDayOfMonthClickListener = View.OnClickListener { view ->
        // Extract day selected
        val dayOfMonthContainer = view as ViewGroup
        var tagId = dayOfMonthContainer.tag as String
        tagId = tagId.substring(DAY_OF_MONTH_CONTAINER.length, tagId.length)
        val dayOfMonthText = view.findViewWithTag(DAY_OF_MONTH_TEXT + tagId) as TextView

        // Fire event
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = firstDayOfWeek
        calendar.time = currentCalendar!!.time
        calendar.set(Calendar.DAY_OF_MONTH, Integer.valueOf(dayOfMonthText.text.toString())!!)
        markDayAsSelectedDay(calendar.time)

        //Set the current day color
        if (!CustomCalendarView.isSameDay((currentCalendar as Calendar), calendar))
            markDayAsCurrentDay(currentCalendar)

        if (dateListener != null) {
            (dateListener as OnDateSelectListener).onDateSelected(calendar.time)
        }
    }

    fun setShowOverflowDate(isOverFlowEnabled: Boolean) {
        isOverflowDateVisible = isOverFlowEnabled
    }

    companion object {

        private val DAY_OF_WEEK = "dayOfWeek"
        private val DAY_OF_MONTH_TEXT = "dayOfMonthText"
        private val DAY_OF_MONTH_CONTAINER = "dayOfMonthContainer"

        /**
         * Checks if a calendar is today.
         *
         * @param calendar the calendar, not altered, not null.
         * @return true if the calendar is today.
         */
        fun isToday(calendar: Calendar): Boolean {
            return isSameDay(calendar, Calendar.getInstance())
        }

        fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
            return cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
        }
    }

    interface OnDateSelectListener {
        fun onDateSelected(date: java.util.Date?)
    }

    interface OnMonthChangedListener {
        fun onMonthChanged(time: java.util.Date?)
    }
}
