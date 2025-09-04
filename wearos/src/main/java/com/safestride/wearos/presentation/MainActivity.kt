package com.safestride.wearos.presentation

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.android.gms.wearable.Wearable
import com.safestride.safestride.shared.WearPaths
import com.safestride.wearos.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    private val io = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<FrameLayout>(R.id.emergencyButton).setOnClickListener {
            sendToPhone(WearPaths.ALERT_RED,  "Emergency pressed")
        }

        findViewById<LinearLayout>(R.id.assistanceButton).setOnClickListener {
            sendToPhone(WearPaths.ALERT_YELLOW, "Assistance pressed")
        }
    }

    private fun sendToPhone(path: String, text: String) {
        io.launch {
            try {
                val nodeClient = Wearable.getNodeClient(this@MainActivity)
                val messageClient = Wearable.getMessageClient(this@MainActivity)

                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "No phone connected", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val payload = text.toByteArray(Charsets.UTF_8)
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, path, payload).await()
                }

                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Sent ✔", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
