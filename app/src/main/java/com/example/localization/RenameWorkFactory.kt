package com.example.localization

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters

class RenameWorkFactory: WorkerFactory() {


    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
     return when(workerClassName){
         "oldWorker" -> WorkServiceOnline(appContext,workerParameters)
         else ->null
     }
    }
}