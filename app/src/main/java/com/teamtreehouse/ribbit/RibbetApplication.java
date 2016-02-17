package com.teamtreehouse.ribbit;


import android.app.Application;

import com.parse.Parse;
import com.parse.ParseObject;

public class RibbetApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId(getString(R.string.parse_application_id))
                .clientKey(getString(R.string.parse_client_secret))
                .server(getString(R.string.server_address))
                .build()
        );

    }
}
