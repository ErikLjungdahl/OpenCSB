package erikljungdahl.opencsb


import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.EditTextPreference.SimpleSummaryProvider.getInstance
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat


class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val pref_doorID : EditTextPreference? = findPreference("doorID")
        pref_doorID?.summaryProvider = getInstance()
        pref_doorID?.setOnPreferenceChangeListener { _: Preference, newValue: Any ->
            createShortcut(newValue.toString())
            true
        }

        val feedback : Preference? = findPreference("feedback")
        feedback?.setOnPreferenceClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ErikLjungdahl/OpenCSB/issues")))
            true
        }
    }

    fun createShortcut (doorID : String) {
        val shortcutManager: ShortcutManager = activity!!.getSystemService(ShortcutManager::class.java)!!
        val shortcut = ShortcutInfo.Builder(activity, "doorID")
            .setShortLabel("AutoOpenCSB")
            .setLongLabel("Automatically OpenCSB")
            .setIcon(Icon.createWithResource(context, R.mipmap.ic_shortcut_autoopencsb))
            .setIntent(
                Intent(activity!!.applicationContext, MainActivity::class.java)
                    .setAction(Intent.ACTION_MAIN)
                    .putExtra("doorID", doorID))
            .build()

        shortcutManager.dynamicShortcuts = listOf(shortcut)
    }
}
