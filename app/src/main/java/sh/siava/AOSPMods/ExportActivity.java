package sh.siava.AOSPMods;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import sh.siava.AOSPMods.utils.PrefManager;
import sh.siava.AOSPMods.utils.SystemUtils;

public class ExportActivity extends AppCompatActivity {

    private static final int REQUEST_IMPORT = 7;
    private static final int REQUEST_EXPORT = 9;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean isExport = getIntent().getBooleanExtra("isExport", false);
        importExportSettings(isExport);
    }

    private void importExportSettings(boolean export) {
        Intent fileIntent = new Intent();
        fileIntent.setAction(export ? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_GET_CONTENT);
        fileIntent.setType("*/*");
        fileIntent.putExtra(Intent.EXTRA_TITLE, "AOSPMods_" + ".bin");
        startActivityForResult(fileIntent, export ? REQUEST_EXPORT : REQUEST_IMPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            finish();
            return;
        } // user hit cancel. Nothing to do
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext().createDeviceProtectedStorageContext());
        switch (requestCode) {
            case REQUEST_IMPORT:
                try {
                    PrefManager.importPath(prefs, getContentResolver().openInputStream(data.getData()));
                    finishAffinity();
                    SystemUtils.RestartLauncher();
                    SystemUtils.RestartSystemUI();
                } catch (Exception ignored) {
                }
                break;
            case REQUEST_EXPORT:
                try {
                    PrefManager.exportPrefs(prefs, getContentResolver().openOutputStream(data.getData()));
                    finish();
                } catch (Exception ignored) {
                }
                break;
        }
    }
}