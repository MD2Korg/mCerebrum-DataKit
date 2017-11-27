package org.md2k.datakit;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import org.md2k.datakit.configuration.ConfigurationManager;
import org.md2k.mcerebrum.commons.dialog.Dialog;
import org.md2k.mcerebrum.commons.dialog.DialogCallback;
import org.md2k.mcerebrum.commons.storage.Storage;
import org.md2k.utilities.FileManager;

import es.dmoral.toasty.Toasty;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class ActivityClear extends AppCompatActivity {
    Subscription subscription;
    private MaterialDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clear);
        clearData();
    }

    void clearData() {
        Dialog.simple(this, "Delete Database & Archive Files?", "Delete Database & Archive Files?\n\nData can't be recovered after deletion", "Yes", "Cancel", new DialogCallback() {
            @Override
            public void onSelected(String value) {
                if (value.equals("Yes"))
                    deleteData();
                else finish();
            }
        }).autoDismiss(true).show();
    }

    void deleteData() {
        subscription = Observable.just(true)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean aBoolean) {
                        dialog = Dialog.progressIndeterminate(ActivityClear.this, "Deleting files...").build();
                        dialog.show();
                        String location = ConfigurationManager.getInstance(ActivityClear.this).configuration.archive.location;
                        String directory = FileManager.getDirectory(ActivityClear.this, location);
                        Storage.deleteDir(directory);
                        location = ConfigurationManager.getInstance(ActivityClear.this).configuration.database.location;
                        if (!directory.equals(FileManager.getDirectory(ActivityClear.this, location))) {
                            directory = FileManager.getDirectory(ActivityClear.this, location);
                            Storage.deleteDir(directory);
                        }
                        return true;
                    }
                }).subscribe(new Observer<Boolean>() {
                    @Override
                    public void onCompleted() {
                        dialog.dismiss();
                        Toasty.success(ActivityClear.this, "Files are deleted.", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Boolean aBoolean) {

                    }
                });
    }
    @Override
    public void onDestroy(){
        if(dialog!=null) dialog.dismiss();
        if(subscription!=null && !subscription.isUnsubscribed())
            subscription.unsubscribe();
        super.onDestroy();
    }

/*
    class DeleteDataAsyncTask extends AsyncTask<String, String, String> {

        DeleteDataAsyncTask() {
            dialog = new ProgressDialog(ActivityClear.this);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Shows Progress Bar Dialog and then call doInBackground method
            dialog.setMessage("Deleting database & archive files. Please wait...");
            dialog.show();
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                String location = ConfigurationManager.getInstance(ActivityClear.this).configuration.archive.location;
                String directory = FileManager.getDirectory(ActivityClear.this, location);
                FileManager.deleteDirectory(directory);
                location = ConfigurationManager.getInstance(ActivityClear.this).configuration.database.location;
                if (!directory.equals(FileManager.getDirectory(ActivityClear.this, location))) {
                    directory = FileManager.getDirectory(ActivityClear.this, location);
                    FileManager.deleteDirectory(directory);
                }
            } catch (Exception ignored) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(String file_url) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
//            Toast.makeText(getActivity(), "Database & Archive files are Deleted", Toast.LENGTH_SHORT).show();
            if (getIntent().getBooleanExtra("delete", false))
                getActivity().finish();
            else
                setPreferences();

        }

    }
*/
}
