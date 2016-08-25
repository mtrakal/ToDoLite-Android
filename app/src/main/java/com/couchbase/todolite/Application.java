package com.couchbase.todolite;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseOptions;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.AuthenticatorFactory;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

public class Application extends android.app.Application implements Replication.ChangeListener {
    public static final String TAG = "ToDoLite";

    private static final String COUCH_DB = "TODO"; // TODO: 25.08.2016
    private static final String COUCH_USER = "TODO";// TODO: 25.08.2016
    private static final String COUCH_PASS = "TODO"; // TODO: 25.08.2016
    private static final String SYNC_URL_HTTP = "https://couch.myapp.com/" + COUCH_DB + "/"; // TODO: 25.08.2016

    // Storage Type: .SQLITE_STORAGE or .FORESTDB_STORAGE
    private static final String STORAGE_TYPE = Manager.FORESTDB_STORAGE;

    // Encryption (Don't store encryption key in the source code. We are doing it here just as an example):
    private static final boolean ENCRYPTION_ENABLED = false;
    private static final String ENCRYPTION_KEY = COUCH_PASS;

    // Logging:
    private static final boolean LOGGING_ENABLED = true;

    // Guest database:
    private static final String GUEST_DATABASE_NAME = "guest";

    private Manager mManager;
    private Database mDatabase;
    private Replication mPull;
    private Replication mPush;
    private Throwable mReplError;
    private String mCurrentUserId;

    @Override
    public void onCreate() {
        super.onCreate();
        enableLogging();
    }

    private void enableLogging() {
        if (LOGGING_ENABLED) {
            Manager.enableLogging(TAG, Log.VERBOSE);
            Manager.enableLogging(Log.TAG, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_SYNC_ASYNC_TASK, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_SYNC, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_QUERY, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_VIEW, Log.VERBOSE);
            Manager.enableLogging(Log.TAG_DATABASE, Log.VERBOSE);
        }
    }

    private Manager getManager() {
        if (mManager == null) {
            try {
                AndroidContext context = new AndroidContext(getApplicationContext());
                mManager = new Manager(context, Manager.DEFAULT_OPTIONS);
            } catch (Exception e) {
                Log.e(TAG, "Cannot create Manager object", e);
            }
        }
        return mManager;
    }

    public Database getDatabase() {
        return mDatabase;
    }

    private void setDatabase(Database database) {
        this.mDatabase = database;
    }

    private Database getUserDatabase(String name) {
        try {
            String dbName = "local-bb-" + name;
            DatabaseOptions options = new DatabaseOptions();
            options.setCreate(true);
            options.setStorageType(STORAGE_TYPE);
            options.setEncryptionKey(ENCRYPTION_ENABLED ? ENCRYPTION_KEY : null);
            return getManager().openDatabase(dbName, options);
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Cannot create database for name: " + name, e);
        }
        return null;
    }


    public void loginAsGuest(Activity activity) {
        setDatabase(getUserDatabase(GUEST_DATABASE_NAME));
        setCurrentUserId(null);
        login(activity);
    }

    public void loginRepl(Activity activity) {
        String userId = COUCH_USER;
        setCurrentUserId(userId);
        setDatabase(getUserDatabase(userId));

        startReplication(AuthenticatorFactory.createBasicAuthenticator(COUCH_USER, COUCH_PASS));
        login(activity);
    }

    private void login(Activity activity) {
        Intent intent = new Intent(activity, ListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    public void logout() {
        setCurrentUserId(null);
        stopReplication();
        setDatabase(null);

        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        intent.setAction(LoginActivity.ACTION_LOGOUT);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void setCurrentUserId(String userId) {
        this.mCurrentUserId = userId;
    }

    public String getCurrentUserId() {
        return this.mCurrentUserId;
    }

    /**
     * Replicator
     */

    private URL getSyncUrl() {
        URL url = null;
        try {
            url = new URL(SYNC_URL_HTTP);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Invalid sync url", e);
        }
        return url;
    }

    private void startReplication(Authenticator auth) {
        if (mPull == null) {
            mPull = mDatabase.createPullReplication(getSyncUrl());
            mPull.setContinuous(true);
            mPull.setAuthenticator(auth);
            mPull.addChangeListener(this);
        }

        if (mPush == null) {
            mPush = mDatabase.createPushReplication(getSyncUrl());
            mPush.setContinuous(true);
            mPush.setAuthenticator(auth);
            mPush.addChangeListener(this);
        }

        mPull.stop();
        mPull.start();

        mPush.stop();
        mPush.start();
    }

    private void stopReplication() {
        if (mPull != null) {
            mPull.removeChangeListener(this);
            mPull.stop();
            mPull = null;
        }

        if (mPush != null) {
            mPush.removeChangeListener(this);
            mPush.stop();
            mPush = null;
        }
    }

    @Override
    public void changed(Replication.ChangeEvent event) {
        Throwable error = null;
        if (mPull != null) {
            if (error == null)
                error = mPull.getLastError();
        }

        if (error == null || error == mReplError)
            error = mPush.getLastError();

        if (error != mReplError) {
            mReplError = error;
            showErrorMessage(mReplError.getMessage(), null);
        }
    }

    /**
     * Display error message
     */

    public void showErrorMessage(final String errorMessage, final Throwable throwable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                android.util.Log.e(TAG, errorMessage, throwable);
                String msg = String.format("%s: %s",
                        errorMessage, throwable != null ? throwable : "");
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void runOnUiThread(Runnable runnable) {
        Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
        mainHandler.post(runnable);
    }
}
