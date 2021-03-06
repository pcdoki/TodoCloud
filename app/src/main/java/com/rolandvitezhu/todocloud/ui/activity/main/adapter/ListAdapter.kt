package com.rolandvitezhu.todocloud.ui.activity.main.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.rolandvitezhu.todocloud.databinding.ItemListBinding
import com.rolandvitezhu.todocloud.di.FragmentScope
import java.util.*
import javax.inject.Inject

@FragmentScope
class ListAdapter @Inject constructor() : BaseAdapter() {

    private val lists: MutableList<com.rolandvitezhu.todocloud.data.List>

    override fun getCount(): Int {
        return lists.size
    }

    override fun getItem(position: Int): Any {
        return lists[position]
    }

    override fun getItemId(position: Int): Long {
        val (_id) = lists[position]
        return _id!!
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView

        val itemListBinding: ItemListBinding
        val layoutInflater = parent.context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE
        ) as LayoutInflater

        if (convertView == null) {
            itemListBinding = ItemListBinding.inflate(
                    layoutInflater,
                    parent,
                    false
            )
            convertView = itemListBinding.root
        } else {
            itemListBinding = convertView.tag as ItemListBinding
        }

        itemListBinding.list = lists[position]
        itemListBinding.executePendingBindings()
        convertView.tag = itemListBinding

        return convertView
    }

    fun update(lists: List<com.rolandvitezhu.todocloud.data.List>?) {
        this.lists.clear()
        this.lists.addAll(lists!!)
    }

    fun clear() {
        lists.clear()
        notifyDataSetChanged()
    }

    fun toggleSelection(position: Int) {
        lists[position].isSelected = isNotSelected(position)
        notifyDataSetChanged()
    }

    fun clearSelection() {
        for (list in lists) {
            list.isSelected = false
        }
        notifyDataSetChanged()
    }

    private fun isNotSelected(position: Int): Boolean {
        return !lists[position].isSelected
    }

    init {
        lists = ArrayList()
    }
}