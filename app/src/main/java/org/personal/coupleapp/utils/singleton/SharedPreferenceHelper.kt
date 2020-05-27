package org.personal.coupleapp.utils.singleton

import android.content.Context

object SharedPreferenceHelper {

    // 패키지 이름으로 key 설정
    private const val STORAGE_KEY = "org.personal.coupleapp"

    fun setString(context: Context, key: String?, value: String?) {
        val editor = context.getSharedPreferences(STORAGE_KEY, Context.MODE_PRIVATE).edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun getString(context: Context, key: String?): String? {
        return context.getSharedPreferences(STORAGE_KEY, Context.MODE_PRIVATE).getString(key, "")
    }

    fun setInt(context: Context, key: String?, value: Int) {
        val editor = context.getSharedPreferences(STORAGE_KEY, Context.MODE_PRIVATE).edit()
        editor.putInt(key, value)
        editor.apply()
    }

    fun getInt(context: Context, key: String?): Int {
        return context.getSharedPreferences(STORAGE_KEY, Context.MODE_PRIVATE).getInt(key, 0)
    }

    fun setBoolean(context: Context, key: String?, value: Boolean) {
        val editor = context.getSharedPreferences(STORAGE_KEY, Context.MODE_PRIVATE).edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    fun getBoolean(context: Context, key: String?): Boolean {
        return context.getSharedPreferences(STORAGE_KEY, Context.MODE_PRIVATE).getBoolean(key, false)
    }

    fun setStringSet(context: Context, key: String?, value: Set<String?>?) {
        val editor = context.getSharedPreferences(STORAGE_KEY, Context.MODE_PRIVATE).edit()
        editor.putStringSet(key, value)
        editor.apply()
    }

    fun getStringSet(context: Context, key: String?): Set<String?>? {
        return context.getSharedPreferences(STORAGE_KEY, Context.MODE_PRIVATE).getStringSet(key, null)
    }
}