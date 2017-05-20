package com.example.todocloud.helper;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

  private static String TAG = SessionManager.class.getSimpleName();
  private SharedPreferences sharedPreferences;
  private SharedPreferences.Editor editor;
  private static final String PREF_NAME = "Login";
  private static final String KEY_IS_LOGGED_IN = "isLoggedIn";

  public SessionManager(Context context) {
    sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    editor = sharedPreferences.edit();
  }

  public void setLogin(boolean isLoggedIn) {
    editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn);
    editor.commit();
  }

  public boolean isLoggedIn() {
    return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
  }

}
