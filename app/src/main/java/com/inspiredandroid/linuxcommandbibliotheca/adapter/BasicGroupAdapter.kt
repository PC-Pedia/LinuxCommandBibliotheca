package com.inspiredandroid.linuxcommandbibliotheca.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.analytics.FirebaseAnalytics
import com.inspiredandroid.linuxcommandbibliotheca.BuildConfig
import com.inspiredandroid.linuxcommandbibliotheca.R
import com.inspiredandroid.linuxcommandbibliotheca.misc.Constants
import com.inspiredandroid.linuxcommandbibliotheca.misc.highlightQueryInsideText
import com.inspiredandroid.linuxcommandbibliotheca.models.CommandChildModel
import com.inspiredandroid.linuxcommandbibliotheca.models.CommandGroupModel
import io.realm.RealmResults
import kotlinx.android.synthetic.main.row_ad.view.*
import kotlinx.android.synthetic.main.row_basic_children.view.*
import kotlinx.android.synthetic.main.row_basic_group.view.*
import java.util.*
import kotlin.collections.ArrayList

class BasicGroupAdapter(private var groups: RealmResults<CommandGroupModel>, private val firebaseAnalytics: FirebaseAnalytics?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val PARENT = 0
        const val CHILDREN = 1
        const val AD = 2
    }

    private val expanded: HashMap<Int, Boolean> = HashMap()
    private var searchQuery = ""
    private var items = ArrayList<BasicItem>()

    class BasicItem(var groupId: Int = 0, var childId: Int = -1)

    init {
        updateItems()
    }

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        return if (item.childId == -1 && item.groupId == -1) {
            AD
        } else if (item.childId == -1) {
            PARENT
        } else {
            CHILDREN
        }
    }

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            PARENT -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.row_basic_group, parent, false)
                return GroupViewHolder(v)
            }
            CHILDREN -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.row_basic_children, parent, false)
                return ChildViewHolder(v)
            }
            AD -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.row_ad, parent, false)
                return AdViewHolder(v)
            }
        }
        throw RuntimeException("No viewType found")
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder.itemViewType) {
            PARENT -> {
                groups[item.groupId]?.let {
                    val groupViewHolder = holder as GroupViewHolder
                    groupViewHolder.bind(it)
                }
            }
            CHILDREN -> {
                groups[item.groupId]?.commands?.get(item.childId)?.let {
                    val childViewHolder = holder as ChildViewHolder
                    childViewHolder.bind(it)
                }
            }
            AD -> {
                val childViewHolder = holder as AdViewHolder
                childViewHolder.bind()
            }
        }
    }

    fun updateItems() {
        items.clear()
        groups.forEachIndexed { index, commandGroupModel ->
            items.add(BasicItem(index))
            if (isExpanded(commandGroupModel.id)) {
                commandGroupModel.commands.forEachIndexed { index2, _ ->
                    items.add(BasicItem(index, index2))
                }
            }
        }
        items.add(BasicItem(-1, -1))
    }

    fun updateData(allGroups: RealmResults<CommandGroupModel>) {
        groups = allGroups
        updateItems()
        notifyDataSetChanged()
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun isExpanded(id: Int): Boolean {
        return expanded[id] == true
    }

    fun trackSelectContent(id: String?) {
        if (BuildConfig.DEBUG) {
            return
        }
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, id)
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Basic Group")
        firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
    }

    fun startShareActivity(context: Context, command: CommandChildModel) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, command.command)
        try {
            context.startActivity(Intent.createChooser(intent, "Share command"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    inner class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: CommandGroupModel) {
            itemView.title.text = item.desc.highlightQueryInsideText(itemView.context, searchQuery).result
            itemView.title.setCompoundDrawablesWithIntrinsicBounds(VectorDrawableCompat.create(itemView.context.resources, item.imageResourceId, null), null, null, null)
            itemView.setOnClickListener { _ ->
                expanded[item.id] = !isExpanded(item.id)
                updateItems()
                notifyDataSetChanged()
                trackSelectContent(item.desc)
            }
        }
    }

    inner class ChildViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(command: CommandChildModel) {
            itemView.description.text = command.command
            itemView.description.setCommands(command.getMansAsStringArray())
            itemView.share.setOnClickListener { view -> startShareActivity(view.context, command) }
        }
    }

    inner class AdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind() {
            itemView.btnQuiz.setOnClickListener {
                startAppMarketActivity(it.context, Constants.PACKAGE_QUIZ)
            }
            itemView.btnRemote.setOnClickListener {
                startAppMarketActivity(it.context, Constants.PACKAGE_LINUXREMOTE)
            }
        }

        /**
         * Show app in the Play Store. If Play Store is not installed, show it in the browser instead.
         *
         * @param appPackageName package mName
         */
        private fun startAppMarketActivity(context: Context, appPackageName: String) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName&referrer=utm_source%3Dlinuxapp%26utm_medium%3Dbasicgroup")))
            } catch (e: android.content.ActivityNotFoundException) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName&referrer=utm_source%3Dlinuxapp%26utm_medium%3Dbasicgroup")))
            }
        }
    }

}