package com.example.localization

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.localization.bubbleWork.BubbleWork


var running:Boolean=false

class MyApplication: Application(){

    companion object {

        lateinit  var appContext: Context

    }
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }

      class ObserverLife: DefaultLifecycleObserver {
          private var bublle: BubbleWork = BubbleWork()


        override fun onResume(owner: LifecycleOwner) {
            super.onResume(owner)
            Log.d(TAGLOG, "onResume: Observer")
            bublle.start(appContext,false, running)

        }

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
            Log.d(TAGLOG, "onStop:Observer")
            bublle.start(appContext,true,running)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            Log.d("MAIN", "onDestroy: Observer")
        }
    }

    }

fun workInfosObserver(): Observer<List<WorkInfo>> {
    return Observer { listOfWorkInfo ->
        if (listOfWorkInfo.isEmpty()) {
            return@Observer
        }
        val workInfo = listOfWorkInfo[0]
        Log.i(TAGLOG, "${workInfo.state}")
        running = workInfo.state==WorkInfo.State.RUNNING
    }
}
