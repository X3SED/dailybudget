package com.dailybudget.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dailybudget.app.ui.BudgetApp
import com.dailybudget.app.ui.BudgetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = Store(applicationContext)
        setContent {
            BudgetTheme {
                BudgetApp(store)
            }
        }
    }
}
