package io.devdeck.sample

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import io.devdeck.DevDeck
import io.devdeck.logs.DeckLogger

class LegacyViewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val padding = (24 * resources.displayMetrics.density).toInt()
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(padding, padding, padding, padding)
                addView(
                    TextView(this@LegacyViewActivity).apply {
                        setText(R.string.legacy_intro)
                        gravity = Gravity.CENTER
                        textSize = 16f
                    },
                )
                addView(
                    Button(this@LegacyViewActivity).apply {
                        setText(R.string.legacy_open_drawer)
                        setOnClickListener { DevDeck.open() }
                    },
                )
                addView(
                    Button(this@LegacyViewActivity).apply {
                        setText(R.string.legacy_toggle_drawer)
                        setOnClickListener {
                            DevDeck.isEnabled = !DevDeck.isEnabled
                            DeckLogger.log("Global drawer enabled = ${DevDeck.isEnabled}")
                        }
                    },
                )
                addView(
                    Button(this@LegacyViewActivity).apply {
                        setText(R.string.legacy_log_analytics)
                        setOnClickListener {
                            DeckLogger.log("screen_viewed", tag = "Analytics", payload = "{\"screen\":\"legacy\"}")
                        }
                    },
                )
            },
        )
    }
}
