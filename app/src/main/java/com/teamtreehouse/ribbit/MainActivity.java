package com.teamtreehouse.ribbit;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Toast;


import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    public ProgressBar mProgressBar;

    public static final int TAKE_PHOTO_REQUEST = 0;
    public static final int TAKE_VIDEO_REQUEST = 1;
    public static final int PICK_PHOTO_REQUEST = 2;
    public static final int PICK_VIDEO_REQUEST = 3;
    public static final int MEDIA_TYPE_IMAGE = 4;
    public static final int MEDIA_TYPE_VIDEO = 5;
    public static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 6;
    public static final int FILE_SIZE_LIMIT = 1024*1024*10; //10MB

    protected Uri mMediaUri;


    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

     //The {@link ViewPager} that will host the section contents.
    private ViewPager mViewPager;

    //OnClickListener for ToolBar Camera button
    protected DialogInterface.OnClickListener mDialogListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            isWriteExternalPermissionGranted();
            switch (which) {
                case 0:
                    takePicture();
                    break;
                case 1:
                    takeVideo();
                    break;
                case 2:
                    choosePhoto();
                    break;
                case 3:
                    chooseVideo();
                    break;
            }
        }

        private void chooseVideo() {
            Intent choosePhotoIntent = new Intent(Intent.ACTION_GET_CONTENT);
            choosePhotoIntent.setType("video/*");
            Toast.makeText(MainActivity.this,
                    getString(R.string.video_file_size_warning), Toast.LENGTH_LONG).show();
            startActivityForResult(choosePhotoIntent, PICK_VIDEO_REQUEST);
        }

        private void choosePhoto() {
            Intent choosePhotoIntent = new Intent(Intent.ACTION_GET_CONTENT);
            choosePhotoIntent.setType("image/*");
            startActivityForResult(choosePhotoIntent, PICK_PHOTO_REQUEST);
        }

        private void takeVideo() {
            Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            mMediaUri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO);
            if (mMediaUri == null) {
                Toast.makeText(MainActivity.this,
                        R.string.error_external_storage,
                        Toast.LENGTH_LONG).show();
            } else {
                videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mMediaUri);
                videoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10);
                //TODO: test if higher video quality is possible
                //https://developer.android.com/reference/android/provider/MediaStore.html#EXTRA_VIDEO_QUALITY
                videoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
                startActivityForResult(videoIntent, TAKE_VIDEO_REQUEST);
            }
        }

        private void takePicture() {
            Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            mMediaUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
            if (mMediaUri == null){
                Toast.makeText(MainActivity.this,
                        R.string.error_external_storage,
                        Toast.LENGTH_LONG).show();
            }else {
                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mMediaUri);
                startActivityForResult(takePhotoIntent, TAKE_PHOTO_REQUEST);
            }
        }

        private Uri getOutputMediaFileUri(int mediaType) {
            // To be safe, check that Memory Storage(SD Card or built-in memory) is mounted
            // using Environment.getExternalStorageState() before doing this.
            if(isExternalStorageAvailable()){// Get URI

                String appName = MainActivity.this.getString(R.string.app_name);
                // 1.get external storage directory
                File mediaStorageDir = new File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        appName);

                // 2.create subdirectory
                if (! mediaStorageDir.exists()) {
                    if (! mediaStorageDir.mkdir()) {
                        Log.e(TAG, "Failed to create directory");
                        return null;
                    }
                }

                // 3.create a filename and file
                File mediaFile;
                Date now = new Date();
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(now);
                String path = mediaStorageDir.getPath() + File.separator;
                if (mediaType == MEDIA_TYPE_IMAGE) {
                    mediaFile = new File(path + "IMG_" + timestamp + ".jpg");
                }else if (mediaType == MEDIA_TYPE_VIDEO){
                    mediaFile = new File(path + "VID_" + timestamp + ".mp4");
                }else {
                    return null;
                }

                //4. return file's URL
                Log.d(TAG, "File: " + Uri.fromFile(mediaFile));
                return Uri.fromFile(mediaFile);
            }else{
                return null;
            }
        }

        private boolean isExternalStorageAvailable() {
            String state = Environment.getExternalStorageState();
            return state.equals(Environment.MEDIA_MOUNTED);
        }
    };

    private boolean isWriteExternalPermissionGranted() {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // We do not have permission to write

                // Should we show an explanation? This method only returns true if the user has previously denied a request.
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // Explain to the user why we need the permission, then prompt for it.
                    // You can do this how you want, I just like snackbars :)
                    showWriteToStorageSnackbar();
                } else {
                    requestWritePermissionWithCallback();

                    // PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }

                // We don't have permission right now, but the user has been prompted.
                return false;
            }
            return true;
        }

        private void showWriteToStorageSnackbar() {
            Snackbar.make(mViewPager, "Write to storage is required to store and access photos/videos.",
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction("OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            requestWritePermissionWithCallback();
                        }
                    }).show();
        }

        private void requestWritePermissionWithCallback() {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Permission was granted, yay!

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    showWriteToStorageSnackbar();
                }
                break;
            default:
                Log.e(TAG, "Got request code: " + requestCode + " which is not used in switch.");
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
        } else {
            Log.i(TAG, currentUser.getUsername());
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_spinner);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_PHOTO_REQUEST || requestCode == PICK_VIDEO_REQUEST){
                if (data == null){
                    Toast.makeText(this, getString(R.string.general_error), Toast.LENGTH_LONG).show();
                } else {
                    mMediaUri = data.getData();
                }
                Log.i(TAG, "Media URI: " + mMediaUri);
                if (requestCode == PICK_VIDEO_REQUEST){
                    int fileSize = 0;
                    InputStream inputStream = null;
                    try {
                        inputStream = getContentResolver().openInputStream(mMediaUri);
                        fileSize = inputStream.available();
                    } catch (FileNotFoundException e) {
                        Toast.makeText(this, R.string.error_opening_file, Toast.LENGTH_LONG).show();
                        return;
                    }  catch (IOException e) {
                        Toast.makeText(this, R.string.error_opening_file, Toast.LENGTH_LONG).show();
                        return;
                    } finally {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    Log.i(TAG, fileSize + "");
                    if (fileSize >= FILE_SIZE_LIMIT) {
                        Toast.makeText(this, getString(R.string.error_file_size_too_large),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            } else {
                //add to Gallery
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(mMediaUri);
                sendBroadcast(mediaScanIntent);
            }

            Intent recipientsIntent = new Intent(this, RecipientsActivity.class);
            recipientsIntent.setData(mMediaUri);

            String fileType;
            if (requestCode == PICK_PHOTO_REQUEST || requestCode == TAKE_PHOTO_REQUEST) {
                fileType = ParseConstants.TYPE_IMAGE;
            } else {
                fileType = ParseConstants.TYPE_VIDEO;
            }
            recipientsIntent.putExtra(ParseConstants.KEY_FILE_TYPE, fileType);
            startActivity(recipientsIntent);

        } else if (resultCode != RESULT_CANCELED){
            Toast.makeText(this, R.string.general_error, Toast.LENGTH_LONG).show();
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_logout:
                ParseUser.logOut();
                navigateToLogin();
            case R.id.action_edit_friends:
                Intent intent = new Intent(this, EditFriendsActivity.class);
                startActivity(intent);
            case R.id.action_camera:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setItems(R.array.camera_choices, mDialogListener);
                AlertDialog dialog = builder.create();
                dialog.show();
        }

        return super.onOptionsItemSelected(item);
    }


}
