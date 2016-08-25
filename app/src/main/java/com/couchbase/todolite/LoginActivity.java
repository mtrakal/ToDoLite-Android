package com.couchbase.todolite;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;


public class LoginActivity extends AppCompatActivity {
    public static final String ACTION_LOGOUT = "logout";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (ACTION_LOGOUT.equals(getIntent().getAction())) {
            logout();
        } else {

            if (isLoggedAsGuest()) {
                loginAsGuest();
                return;
            }
        }

        Button connect = (Button) findViewById(R.id.connect);
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                login();
            }
        });

        Button guestLoginButton = (Button) findViewById(R.id.guest_login_button);
        guestLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginAsGuest();
            }
        });
    }

    private void login() {
        Application application = (Application) getApplication();
        application.loginRepl(this);
    }

    private void loginAsGuest() {
        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("guest", true);
        editor.commit();
        Application application = (Application) getApplication();
        application.loginAsGuest(this);
    }

    private boolean isLoggedAsGuest() {
        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
        return pref.getBoolean("guest", false);
    }

    private void logout() {
        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.remove("guest");
        editor.commit();
    }
}
