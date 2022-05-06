package com.fansan.filemodifytime

import android.annotation.SuppressLint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

/**
 *@author  fansan
 *@version 2022/3/3
 */

class MainViewModel : ViewModel() {
    val isRoot
        get() = currentPath == PathUtils.getExternalStoragePath()

    var fileChooseMode by mutableStateOf(false)
    var currentPath: String by mutableStateOf("")
    var pathCache: MutableMap<String, MutableList<FileItem>> = mutableMapOf()
    var loading by mutableStateOf(false)
    val historyPath = mutableListOf<String>()
    var selectedPath = ""
    var parentPosition = mutableMapOf<String,IndexBean>()
    var testKey by mutableStateOf("0")
    var analysis by mutableStateOf(false)
    var jumpCount by mutableStateOf(0)
    var errorCount by mutableStateOf(0)
    var noMatchCount by mutableStateOf(0)
    var matchCount by mutableStateOf(0)
    var noMatchFiles = mutableListOf<File>()
    var fixing by mutableStateOf(false)
    var done by mutableStateOf(false)
    var successCount = 0
    var failedCount = 0
    var currentFileIndex = 0
    var selectedDirCount = 0
    var jumpFiles = mutableListOf<File>()
    var errorFiles = mutableListOf<File>()

    fun updataTest():Flow<MutableList<String>> {
        return flow {
            val list = mutableListOf<String>()
            for (i in 0..50){
                  list.add((0..100).random().toString())
            }
            emit(list)
        }
    }

    fun updataDirs(): Flow<MutableList<FileItem>> {
        return flow {
            val result = pathCache.getOrElse(currentPath) {
                val fileList = FileUtils.listFilesInDirWithFilter(currentPath) {
                    it.isDirectory
                }
                val mapResult = fileList?.map {
                    FileItem(it.name, it.absolutePath, currentPath)
                }?.toMutableList()

                if (currentPath != PathUtils.getExternalStoragePath())
                    mapResult?.add(0, FileItem("", "", currentPath, 0))

                mapResult
            }

            result?.let {
                pathCache[currentPath] = it
            }
            loading = false
            if (result != null) {
                emit(result)
            }
        }.flowOn(Dispatchers.IO)
            .onStart {
                loading = true
            }.onCompletion {
                loading = false
            }
            .catch {
                emit(mutableListOf())
            }
    }

    fun analysisFile(){
        viewModelScope.launch(Dispatchers.IO){
            val list = FileUtils.listFilesInDir(selectedPath)
            selectedDirCount = list.size
            analysis = true
            list.forEachIndexed { index, file ->
                currentFileIndex = index+1
                if (file.isDirectory){
                    jumpFiles.add(file)
                    jumpCount++
                    return@forEachIndexed
                }

                val exifInterface = ExifInterface(file)
                val dataTime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME)
                if (dataTime == null) {
                    errorCount++
                    errorFiles.add(file)
                    return@forEachIndexed
                }
                val exifMillis =
                    TimeUtils.string2Millis(dataTime, "yyyy:MM:dd HH:mm:ss")
                if (exifMillis != file.lastModified()){
                    noMatchCount++
                    noMatchFiles.add(file)
                    return@forEachIndexed
                }
                matchCount++
            }
            analysis = false
        }
    }

    fun fixFiles(){
        viewModelScope.launch(Dispatchers.IO){
            fixing = true
            noMatchFiles.forEach {
                val exifInterface = ExifInterface(it)
                val dataTime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME)
                val exifMillis =
                    TimeUtils.string2Millis(dataTime, "yyyy:MM:dd HH:mm:ss")
                val result = it.setLastModified(exifMillis)
                if (result) successCount++ else failedCount++
            }
            fixing = false
            jumpCount = 0
            errorCount = 0
            noMatchCount = 0
            currentFileIndex = 0
            matchCount = 0
            selectedPath = ""
            selectedDirCount = 0
            noMatchFiles.clear()
            done = true
        }

    }

    fun infomation(){
        viewModelScope.launch(Dispatchers.IO){
            errorFiles.forEach {
                val exifInterface = ExifInterface(it)
                LogUtils.d("fansangg","${it.path} -- ${exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)} -- ${exifInterface.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)} -- ${exifInterface.getAttribute(ExifInterface.TAG_DATETIME)}")
            }
        }
    }
}