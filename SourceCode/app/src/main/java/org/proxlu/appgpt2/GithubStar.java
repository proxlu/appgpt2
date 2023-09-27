package org.proxlu.appgpt2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

public class GithubStar {
    public static void setAskForStar(boolean askForStar, Context context){
        SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefManager.edit();
        editor.putBoolean("askForStar", askForStar);
        editor.apply();
    }

    static boolean shouldShowStarDialog(Context context) {
        SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(context);
        int versionCode = prefManager.getInt("versionCode",0);
        boolean askForStar=prefManager.getBoolean("askForStar",true);

        if (prefManager.contains("versionCode") && BuildConfig.VERSION_CODE>versionCode && askForStar){ //not at first start, only after upgrade and only if use has not yet given a star or has declined
            SharedPreferences.Editor editor = prefManager.edit();
            editor.putInt("versionCode", BuildConfig.VERSION_CODE);
            editor.apply();
            return true;
        } else {
            SharedPreferences.Editor editor = prefManager.edit();
            editor.putInt("versionCode", BuildConfig.VERSION_CODE);
            editor.apply();
            return false;
        }
    }

    static void starDialog(Context context){
        SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefManager.getBoolean("askForStar",true))    {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
            alertDialogBuilder.setMessage(R.string.dialog_StarOnGitHub);
            alertDialogBuilder.setPositiveButton(context.getString(R.string.dialog_OK_button), (dialog, which) -> {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/proxlu/AppGPT")));
                setAskForStar(false, context);
            });
            alertDialogBuilder.setNegativeButton(context.getString(R.string.dialog_NO_button), (dialog, which) -> setAskForStar(false, context));
            alertDialogBuilder.setNeutralButton(context.getString(R.string.dialog_Later_button), null);

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    }

}
