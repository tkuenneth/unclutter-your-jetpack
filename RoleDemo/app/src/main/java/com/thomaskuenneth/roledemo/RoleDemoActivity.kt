package com.thomaskuenneth.roledemo

import android.app.role.RoleManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.role.RoleManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RoleDemoActivity : ComponentActivity() {

    private val roleMessage: MutableStateFlow<String> = MutableStateFlow("")
    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            when (it.resultCode) {
                RESULT_OK -> roleMessage.update { getString(R.string.role_acquired) }
                RESULT_CANCELED -> roleMessage.update { getString(R.string.cancelled) }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = getSystemService(RoleManager::class.java)
        manager?.run {
            if (isRoleAvailable(RoleManagerCompat.ROLE_BROWSER)) {
                roleMessage.update {
                    if (isRoleHeld(RoleManagerCompat.ROLE_BROWSER)) {
                        getString(R.string.has_role)
                    } else {
                        ""
                    }
                }
            } else roleMessage.update { getString(R.string.role_not_available) }
        }
        val requestRole = {
            val intent =
                manager?.createRequestRoleIntent(RoleManagerCompat.ROLE_BROWSER) ?: Intent()
            launcher.launch(intent)
        }

        setContent {
            val scope = rememberCoroutineScope()
            val message by roleMessage.collectAsState()
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Button(onClick = {
                    scope.launch { requestRole() }
                }) {
                    Text(stringResource(id = R.string.acquire_role))
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
