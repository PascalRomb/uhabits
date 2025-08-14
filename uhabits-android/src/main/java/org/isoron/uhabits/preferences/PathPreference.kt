package org.isoron.uhabits.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.core.preferences.Preferences

class PathPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {


    override fun getSummary(): CharSequence? {
        val preferences: Preferences = (context.applicationContext as HabitsApplication).component.preferences
        if(preferences.backupPath.isNotBlank()) {
            return  context.getString(R.string.auto_export_path_selection_summary_selected) + preferences.backupPath
        }
        return context.getString(R.string.auto_export_path_selection_summary_empty)
    }

}