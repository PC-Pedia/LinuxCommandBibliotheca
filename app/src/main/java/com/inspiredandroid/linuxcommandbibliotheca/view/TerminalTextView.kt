package com.inspiredandroid.linuxcommandbibliotheca.view

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.Typeface
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v7.widget.AppCompatTextView
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.AttributeSet

import com.inspiredandroid.linuxcommandbibliotheca.R
import com.inspiredandroid.linuxcommandbibliotheca.interfaces.OnLinkClickListener
import com.inspiredandroid.linuxcommandbibliotheca.misc.FragmentCoordinator

/**
 * Created by Simon Schubert
 *
 *
 * This View makes it very easy to highlightQueryInsideText defined commands in an normal textview. Define the
 * commands which should be highlighted in an string array and link it in the layout resource as
 * "command".
 */
class TerminalTextView(context: Context, attrs: AttributeSet) : AppCompatTextView(context, attrs) {

    private var commands: Array<String> = arrayOf()
    private val outputRows: IntArray

    private val activity: FragmentActivity?
        get() {
            var context = context
            while (context is ContextWrapper) {
                if (context is FragmentActivity) {
                    return context
                }
                context = context.baseContext
            }
            return null
        }

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.TerminalTextView)
        val resID = ta.getResourceId(R.styleable.TerminalTextView_commands, R.array.default_codetextview_commands)
        val outputRowsResID = ta.getResourceId(R.styleable.TerminalTextView_outputRows, R.array.default_codetextview_commands)
        commands = context.resources.getStringArray(resID)
        outputRows = context.resources.getIntArray(outputRowsResID)
        ta.recycle()

        if (!isInEditMode) {
            updateLinks()
            movementMethod = LinkMovementMethod.getInstance()
            highlightColor = Color.TRANSPARENT
        }
    }

    /**
     * Set clickable man pages(commands)
     *
     * @param commands
     */
    fun setCommands(commands: Array<String>) {
        this.commands = commands
        updateLinks()
    }

    /**
     * Mark man pages(commands) clickable
     */
    private fun updateLinks() {
        text = createSpannable(text.toString(), commands)
    }

    /**
     * Highlights Commands of the text and make them clickable
     *
     * @param text     spannable content
     * @param commands list of commands to highlightQueryInsideText
     * @return
     */
    private fun createSpannable(text: String, commands: Array<String>): SpannableString {
        val ss = SpannableString(text)

        for (command in commands) {
            val listener = object : OnLinkClickListener {
                override fun onLinkClick() {
                    FragmentCoordinator.startCommandManActivity(activity, command)
                }
            }
            ClickableTextView.addClickableSpanToPhrases(ss, text, command, listener)
        }

        addItalicSpans(ss, text)
        addOutputSpans(ss, text)

        return ss
    }

    /**
     * @param ss
     * @param text
     */
    private fun addOutputSpans(ss: SpannableString, text: String) {
        if (outputRows.isEmpty()) {
            return
        }

        val lines = text.split("\\r?\\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var start = 0
        var end = 0

        for (i in lines.indices) {
            end += lines[i].length
            if (outputRows.any { it == i }) {
                ss.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.grey)), start, end, 0)
            }
            end += 1
            start = end
        }
    }

    /**
     * Make placeholder text italic
     *
     * @param ss
     * @param text
     */
    private fun addItalicSpans(ss: SpannableString, text: String) {
        var indexStart = 0
        while (text.indexOf("[", indexStart) != -1) {
            val start = text.indexOf("[", indexStart)
            val end = text.indexOf("]", indexStart)
            if (start == -1 || end == -1 || start >= end) {
                break
            }
            ss.setSpan(StyleSpan(Typeface.ITALIC), start, end + 1, 0)
            indexStart = end + 1
        }
    }
}