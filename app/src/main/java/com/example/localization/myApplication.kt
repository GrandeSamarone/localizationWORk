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
        override fun onCreate(owner: LifecycleOwner) {
            super.onCreate(owner)
            Log.d("MAIN", "onCreate: Applicationbserver ")
        }

        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            Log.d("MAIN","onStart: Applicationbserver")
        }

        override fun onPause(owner: LifecycleOwner) {
            super.onPause(owner)
            Log.d("MAIN", "onPause: Applicationbserver")


        }

        override fun onResume(owner: LifecycleOwner) {
            super.onResume(owner)
            Log.d("MAIN", "onResume: Applicationbserver")
            bublle.start(appContext,false, running)

        }

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
            Log.d("MAIN", "onStop: Applicationbserver")
            bublle.start(appContext,true,running)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            Log.d("MAIN", "onDestroy: Applicationbserver")
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
        // binding.textobs.text = workInfo.state.toString()
        // binding.texterror.text = workInfo.outputData.getString("error")
    }
}
