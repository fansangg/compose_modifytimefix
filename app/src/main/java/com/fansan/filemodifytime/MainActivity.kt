package com.fansan.filemodifytime

import android.inputmethodservice.Keyboard
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.PathUtils
import com.fansan.filemodifytime.ui.theme.FileModifyTimeTheme
import com.fansan.filemodifytime.ui.theme.Purple500
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val mViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FileModifyTimeTheme {
                MainView(chooseFileClick = {
                    checkStoragePermission {
                        mViewModel.currentPath = PathUtils.getExternalStoragePath()
                        mViewModel.fileChooseMode = true
                    }
                }, confirmClick = {
                    mViewModel.selectedPath = it
                    mViewModel.fileChooseMode = false
                    mViewModel.currentPath = ""
                    mViewModel.historyPath.clear()
                })
            }
        }
    }

    override fun onBackPressed() {
        if (mViewModel.fileChooseMode) {
            if (mViewModel.isRoot) {
                mViewModel.parentPosition.clear()
                mViewModel.fileChooseMode = false
            } else {
                mViewModel.parentPosition.remove(mViewModel.currentPath)
                mViewModel.currentPath = mViewModel.historyPath.removeLast()
            }
        } else
            super.onBackPressed()
    }

    private fun checkStoragePermission(func: () -> Unit) {
        XXPermissions.with(this)
            .permission(Permission.MANAGE_EXTERNAL_STORAGE)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                    if (all) {
                        func()
                    }
                }

                override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                    if (never) {
                        Toast.makeText(this@MainActivity, "没有权限", Toast.LENGTH_LONG).show()
                        XXPermissions.startPermissionActivity(this@MainActivity, permissions)
                    } else
                        Toast.makeText(this@MainActivity, "获取权限失败", Toast.LENGTH_LONG).show()
                }

            })

    }
}

@Composable
fun MainView(chooseFileClick: () -> Unit, confirmClick: (currentPath: String) -> Unit) {
    val scaffoldState = rememberScaffoldState()
    val mainViewModel = viewModel<MainViewModel>()
    val mainTitle = "🔧 媒体文件时间修复工具"
    val fileListTitle = "选择一个文件夹"
    Scaffold(topBar = {
        TopAppBar(title = {
            Text(
                text = if (mainViewModel.fileChooseMode) fileListTitle else mainTitle,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }, backgroundColor = Purple500, actions = {
            if (mainViewModel.fileChooseMode)
                Text(
                    text = "确定",
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier
                        .padding(15.dp)
                        .clickable(onClick = { confirmClick(mainViewModel.currentPath) })
                )
        })
    }, scaffoldState = scaffoldState) {
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(start = 15.dp, end = 15.dp)) {
            ChooseFileBtn(click = chooseFileClick)
            if (mainViewModel.fileChooseMode) {
                FileList()
            }

            if (mainViewModel.loading)
                LoadingView(modifier = Modifier.align(alignment = Alignment.Center))
        }
    }
}

@Composable
fun LoadingView(modifier: Modifier) {
    CircularProgressIndicator(
        modifier = modifier
            .size(40.dp)
    )
}

@Composable
fun ChooseFileBtn(click: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clickable(onClick = click)
                .background(color = Color.LightGray, shape = RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "选择文件夹",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 15.dp)
            )
        }
    }

}

@Composable
fun FileList() {
    val mainViewModel = viewModel<MainViewModel>()
    val lazyColumnListState = rememberLazyListState()
    if (mainViewModel.currentPath.isNotEmpty()) {
        val datas by mainViewModel.updataDirs().collectAsState(initial = mutableListOf())
        LazyColumn(content = {
            items(items = datas) {
                FileListItem(item = it) {
                    if (it.viewType == 1) {
                        mainViewModel.parentPosition[it.parent] = IndexBean(lazyColumnListState.firstVisibleItemIndex,lazyColumnListState.firstVisibleItemScrollOffset)
                        mainViewModel.historyPath.add(it.parent)
                        mainViewModel.currentPath = it.path
                    } else {
                        mainViewModel.parentPosition.remove(it.path)
                        mainViewModel.currentPath = mainViewModel.historyPath.removeLast()
                    }
                }
            }
        }, state = lazyColumnListState, modifier = Modifier.background(color = Color.White))

        LaunchedEffect(key1 = mainViewModel.currentPath, block = {
            val data = mainViewModel.parentPosition[mainViewModel.currentPath]
            if (data != null){
                lazyColumnListState.scrollToItem(data.index,data.offset)
            }else{
                lazyColumnListState.scrollToItem(0,0)
            }
        })
    }
}

@Composable
fun FileListItem(item: FileItem, click: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(vertical = 10.dp, horizontal = 5.dp)
            .noRippleClick(click = click),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .wrapContentWidth(), contentAlignment = Alignment.Center
        ) {
            Image(imageVector = if (item.viewType == 1) Icons.Default.Star else Icons.Default.Home, contentDescription = "file")
        }
        Column(
            verticalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.padding(start = 10.dp)
        ) {
            if (item.viewType == 1) {
                Text(
                    text = item.dirName,
                    fontSize = 15.sp,
                    color = Color(0xff333333),
                    fontWeight = FontWeight.SemiBold,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.path,
                    fontSize = 13.sp,
                    color = Color(0xff333333),
                    fontWeight = FontWeight.Light,
                    overflow = TextOverflow.Clip
                )
            } else
                Text(
                    text = "上一页",
                    fontSize = 15.sp,
                    color = Color(0xff333333),
                    fontWeight = FontWeight.SemiBold,
                    overflow = TextOverflow.Ellipsis
                )
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    /*FileModifyTimeTheme {
        MainView(title = "选择文件", mode = true)
    }*/

    /*Box(Modifier.fillMaxSize()) {
        LoadingView(modifier = Modifier.align(alignment = Alignment.Center))
    }*/

    //FileListItem(item = FileItem("hahah", "1/1/1/1/11/1/1/1/11/1/1/1/1", "111", 1))
}