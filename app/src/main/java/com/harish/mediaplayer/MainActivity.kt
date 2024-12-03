package com.harish.mediaplayer

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.harish.mediaplayer.navigation.AppNavHost
import com.harish.mediaplayer.ui.theme.MediaPlayerTheme
import com.harish.mediaplayer.util.MediaPlayerService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MediaPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionRequestScreen(context = this)
                }
            }
        }
    }

    // Check if the app has permission to read media audio (for Android 13+)
    fun checkPermissions(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 and above, check for READ_MEDIA_AUDIO permission
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            // For versions below Android 13, use READ_EXTERNAL_STORAGE
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Request the necessary permission for reading media audio or external storage
    fun requestPermission(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 and above, request READ_MEDIA_AUDIO
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // For versions below Android 13, request READ_EXTERNAL_STORAGE
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, navigate to the main content screen
                setContent {
                    MediaPlayerTheme {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            AppNavHost()
                        }
                    }
                }
            } else {
                // Permission denied, show a message or handle accordingly
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val PERMISSION_REQUEST_CODE = 100
    }
}

@Composable
fun PermissionRequestScreen(context: Context) {
    var permissionGranted by remember { mutableStateOf(false) }

    // Check if permission is already granted
    LaunchedEffect(Unit) {
        permissionGranted = (context as MainActivity).checkPermissions(context)
    }

    if (permissionGranted) {
        // If permission is granted, navigate to main screen
        AppNavHost()
    } else {
        // Show permission request screen
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "We need permission to access media files.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(
                onClick = { (context as MainActivity).requestPermission(context) },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Grant Permission")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MediaPlayerTheme {
        PermissionRequestScreen(context = LocalContext.current)
    }
}