package com.rolandvitezhu.todocloud.ui.activity.main.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rolandvitezhu.todocloud.R
import com.rolandvitezhu.todocloud.app.AppController.Companion.appContext
import com.rolandvitezhu.todocloud.app.AppController.Companion.instance
import com.rolandvitezhu.todocloud.data.PredefinedList
import com.rolandvitezhu.todocloud.datastorage.DbLoader
import java.util.*
import javax.inject.Inject

class PredefinedListsViewModel : ViewModel() {

    @Inject
    lateinit var dbLoader: DbLoader

    private val _predefinedLists = MutableLiveData<List<PredefinedList>>()
    val predefinedLists: LiveData<List<PredefinedList>>
        get() = _predefinedLists
    var predefinedList = PredefinedList()

    /**
     * Set the list of predefined lists.
     */
    fun updatePredefinedListsViewModel() {
        if (appContext != null)
        {
            val todayPredefinedListWhere: String = dbLoader.prepareTodayPredefinedListWhere()
            val next7DaysPredefinedListWhere: String = dbLoader.prepareNext7DaysPredefinedListWhere()
            val allPredefinedListWhere: String = dbLoader.prepareAllPredefinedListWhere()
            val completedPredefinedListWhere: String = dbLoader.prepareCompletedPredefinedListWhere()

            val predefinedLists = ArrayList<PredefinedList>()

            predefinedLists.add(
                    PredefinedList(appContext!!.getString(R.string.all_today), todayPredefinedListWhere))
            predefinedLists.add(
                    PredefinedList(appContext!!.getString(R.string.all_next7days), next7DaysPredefinedListWhere))
            predefinedLists.add(
                    PredefinedList(appContext!!.getString(R.string.all_all), allPredefinedListWhere))
            predefinedLists.add(
                    PredefinedList(appContext!!.getString(R.string.all_completed), completedPredefinedListWhere))

            _predefinedLists.value = predefinedLists
        }
    }

    init {
        instance?.appComponent?.inject(this)
    }
}