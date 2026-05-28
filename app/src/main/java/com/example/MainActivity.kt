package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.SubscriptionRepository
import com.example.ui.SubscriptionTrackerApp
import com.example.ui.SubscriptionViewModel
import com.example.ui.SubscriptionViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    
    // Lazy build the Room Database
    private val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "subsentry_database"
        ).fallbackToDestructiveMigration()
         .build()
    }

    // Lazy instantiate the repository supplying the secure BuildConfig.GEMINI_API_KEY
    private val repository by lazy {
        SubscriptionRepository(
            subscriptionDao = database.subscriptionDao(),
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    // Initialize the ViewModel using our custom VM Factory
    private val viewModel: SubscriptionViewModel by viewModels {
        SubscriptionViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup modern edge to edge display rules
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                SubscriptionTrackerApp(viewModel = viewModel)
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun Greeting(name: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}
