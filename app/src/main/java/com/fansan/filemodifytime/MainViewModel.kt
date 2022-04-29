package com.fansan.filemodifytime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Path
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
}