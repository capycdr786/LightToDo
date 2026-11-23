package com.capycdr786.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.capycdr786.todo.ui.screens.HomeScreen
import com.capycdr786.todo.ui.theme.MinimalisttodoTheme
import com.capycdr786.todo.ui.viewmodel.TaskViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val taskViewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MinimalisttodoTheme {
                HomeScreen(viewModel = taskViewModel)
            }
        }
    }
}
