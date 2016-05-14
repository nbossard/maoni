package org.rm3l.maoni.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.rm3l.maoni.BuildConfig;
import org.rm3l.maoni.MaoniConfiguration;
import org.rm3l.maoni.R;
import org.rm3l.maoni.model.Feedback;

import java.io.File;
import java.lang.reflect.Method;
import java.util.UUID;

public class MaoniActivity extends AppCompatActivity {

    public static final String SCREENSHOT_FILE = "SCREENSHOT_FILE";
    public static final String CALLER_ACTIVITY = "CALLER_ACTIVITY";
    public static final String WINDOW_TITLE = "WINDOW_TITLE";
    public static final String MESSAGE = "MESSAGE";
    public static final String HEADER = "HEADER";
    public static final String SCREENSHOT_HINT = "SCREENSHOT_HINT";
    public static final String CONTENT_HINT = "CONTENT_HINT";
    public static final String SCREENSHOT_TOUCH_TO_PREVIEW_HINT = "SCREENSHOT_PREVIEW_HINT";
    public static final String INCLUDE_SCREENSHOT_TEXT = "INCLUDE_SCREENSHOT_TEXT";

    @Nullable
    private TextInputLayout mContentInputLayout;

    @Nullable
    private EditText mContent;

    @Nullable
    private CheckBox mIncludeScreenshot;

    @Nullable
    private CharSequence mScreenshotFilePath;

    protected View mRootView;
    private Menu mMenu;

    private String mFeedbackUniqueId;
    private Feedback.App mAppInfo;
    private Feedback.Phone mPhoneInfo;

    private MaoniConfiguration.Validator mValidator;
    private MaoniConfiguration.Listener mListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(R.style.AppTheme_NoActionBar);

        setContentView(R.layout.maoni_activity_feedback);

        mRootView = findViewById(R.id.maoni_container);
        if (mRootView == null) {
            throw new IllegalStateException(
                    "Layout must contain a root view with the following id: maoni_container");
        }

        final Intent intent = getIntent();

        final ImageView headerImageView = (ImageView) findViewById(R.id.maoni_toolbar_header_image);
        if (headerImageView != null && intent.hasExtra(HEADER)) {
            final int headerLayoutId = intent.getIntExtra(HEADER, -1);
            if (headerLayoutId != -1) {
                headerImageView.setImageResource(headerLayoutId);
            }
        }

        final MaoniConfiguration maoniConfiguration = MaoniConfiguration.getInstance();

        final Integer extraLayout = maoniConfiguration.getExtraLayout();
        if (extraLayout != null) {
            final View extraContentView = findViewById(R.id.maoni_content_extra);
            if (extraContentView instanceof LinearLayout) {
                final LinearLayout extraContent = (LinearLayout) extraContentView;
                extraContent.setVisibility(View.VISIBLE);
                extraContent
                        .addView(getLayoutInflater().inflate(extraLayout, extraContent, false));
            }
        }

        mListener = maoniConfiguration.getListener();
        mValidator = maoniConfiguration.getValidator();

        final Context applicationContext = getApplicationContext();

        final Toolbar toolbar = (Toolbar) findViewById(R.id.maoni_toolbar);
        if (toolbar != null) {
            toolbar.setTitle(intent.hasExtra(WINDOW_TITLE) ?
                    intent.getCharSequenceExtra(WINDOW_TITLE) :
                    getString(R.string.send_feedback));
            toolbar.setTitleTextAppearance(applicationContext,
                    R.style.MaoniTheme_ToolbarTitle);
            toolbar.setSubtitleTextAppearance(applicationContext,
                    R.style.MaoniTheme_ToolbarSubtitle);
            toolbar.setTitleTextColor(ContextCompat.getColor(this,
                    R.color.white));
            toolbar.setSubtitleTextColor(ContextCompat.getColor(this,
                    R.color.white));
            setSupportActionBar(toolbar);
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        if (intent.hasExtra(MESSAGE)) {
            final CharSequence message = intent.getCharSequenceExtra(MESSAGE);
            final TextView activityMessageTv = (TextView) findViewById(R.id.maoni_feedback_message);
            if (activityMessageTv != null) {
                activityMessageTv.setText(message);
            }
        }

        if (intent.hasExtra(SCREENSHOT_HINT)) {
            final CharSequence screenshotInformationalHint = intent.getCharSequenceExtra(SCREENSHOT_HINT);
            final TextView screenshotInformationalHintTv =
                    (TextView) findViewById(R.id.maoni_screenshot_informational_text);
            if (screenshotInformationalHintTv != null) {
                screenshotInformationalHintTv.setText(screenshotInformationalHint);
            }
        }

        mContentInputLayout = (TextInputLayout) findViewById(R.id.maoni_content_input_layout);
        mContent = (EditText) findViewById(R.id.maoni_content);

        if (mContent != null && intent.hasExtra(CONTENT_HINT)) {
            mContent.setHint(intent.getCharSequenceExtra(CONTENT_HINT));
        }

        mIncludeScreenshot = (CheckBox) findViewById(R.id.maoni_include_screenshot);
        if (mIncludeScreenshot != null && intent.hasExtra(INCLUDE_SCREENSHOT_TEXT)) {
            mIncludeScreenshot.setText(intent.getCharSequenceExtra(INCLUDE_SCREENSHOT_TEXT));
        }

        final ImageButton screenshotThumb = (ImageButton)
                findViewById(R.id.maoni_screenshot);

        final TextView touchToPreviewTextView =
                (TextView) findViewById(R.id.maoni_screenshot_touch_to_preview);
        if (touchToPreviewTextView != null && intent.hasExtra(SCREENSHOT_TOUCH_TO_PREVIEW_HINT)) {
            touchToPreviewTextView.setText(
                    intent.getCharSequenceExtra(SCREENSHOT_TOUCH_TO_PREVIEW_HINT));
        }

        final View screenshotContentView =
                findViewById(R.id.maoni_include_screenshot_content);
        mScreenshotFilePath = intent.getCharSequenceExtra(SCREENSHOT_FILE);
        if (!TextUtils.isEmpty(mScreenshotFilePath)) {
            final File file = new File(mScreenshotFilePath.toString());
            if (file.exists()) {
                if (mIncludeScreenshot != null) {
                    mIncludeScreenshot.setVisibility(View.VISIBLE);
                }
                if (screenshotContentView != null) {
                    screenshotContentView.setVisibility(View.VISIBLE);
                }
                final Bitmap mBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                if (screenshotThumb != null) {
                    screenshotThumb.setImageBitmap(mBitmap);
                }

                // Hook up clicks on the thumbnail views.
                if (screenshotThumb != null) {
                    screenshotThumb.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            final Dialog imagePreviewDialog = new Dialog(MaoniActivity.this);

                            imagePreviewDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                            imagePreviewDialog.getWindow().setBackgroundDrawable(
                                    new ColorDrawable(android.graphics.Color.TRANSPARENT));

                            imagePreviewDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialogInterface) {
                                    //nothing;
                                }
                            });

                            imagePreviewDialog.setContentView(R.layout.maoni_screenshot_preview);

                            final ImageView imageView = (ImageView)
                                    imagePreviewDialog.findViewById(R.id.maoni_screenshot_preview_image);
                            imageView.setImageBitmap(mBitmap);
                            imagePreviewDialog.findViewById(R.id.maoni_screenshot_preview_close)
                                    .setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            imagePreviewDialog.dismiss();
                                        }
                                    });

                            imagePreviewDialog.setCancelable(true);
                            imagePreviewDialog.setCanceledOnTouchOutside(true);

                            imagePreviewDialog.show();
                        }
                    });
                }
            } else {
                if (mIncludeScreenshot != null) {
                    mIncludeScreenshot.setVisibility(View.GONE);
                }
                if (screenshotContentView != null) {
                    screenshotContentView.setVisibility(View.GONE);
                }
            }
        } else {
            if (mIncludeScreenshot != null) {
                mIncludeScreenshot.setVisibility(View.GONE);
            }
            if (screenshotContentView != null) {
                screenshotContentView.setVisibility(View.GONE);
            }
        }

        mFeedbackUniqueId = UUID.randomUUID().toString();

        final View fab = findViewById(R.id.maoni_fab);
        if (fab != null) {
            final ViewTreeObserver viewTreeObserver = fab.getViewTreeObserver();
            if (viewTreeObserver == null) {
                if (this.mMenu != null) {
                    final MenuItem item = this.mMenu.findItem(R.id.maoni_feedback_send);
                    if (item != null) {
                        item.setVisible(false);
                    }
                }
            } else {
                viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mMenu != null) {
                            final MenuItem item = mMenu.findItem(R.id.maoni_feedback_send);
                            if (item != null) {
                                item.setVisible(fab.getVisibility() != View.VISIBLE);
                            }
                        }
                    }
                });
            }
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    validateAndSubmitForm();
                }
            });
        }

        setAppRelatedInfo();
        setPhoneRelatedInfo();

        final MaoniConfiguration.UiListener uiListener = maoniConfiguration.getUiListener();
        if (uiListener != null) {
            uiListener.onCreate(mRootView, savedInstanceState);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.maoni_activity_menu, menu);
        this.mMenu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    private void setAppRelatedInfo() {

        // Set app related properties
        final PackageManager manager = getPackageManager();
        PackageInfo info = null;
        try {
            info = manager.getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            //No worries
        }

        final CharSequence callerActivity = getIntent().getCharSequenceExtra(CALLER_ACTIVITY);
        mAppInfo = new Feedback.App(
                callerActivity != null ? callerActivity : getClass().getSimpleName(),
                BuildConfig.DEBUG,
                BuildConfig.APPLICATION_ID,
                info != null ? info.versionCode : BuildConfig.VERSION_CODE,
                BuildConfig.FLAVOR,
                BuildConfig.BUILD_TYPE,
                info != null ? info.versionName : BuildConfig.VERSION_NAME);
    }

    private void setPhoneRelatedInfo() {

        // Set phone related properties
        SupplicantState supplicantState = null;
        try {
            final WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            @SuppressWarnings("MissingPermission")
            final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            supplicantState = wifiInfo.getSupplicantState();
        } catch (Exception e) {
            //No worries
        }

        boolean mobileDataEnabled = false; // Assume disabled
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            Class cmClass = Class.forName(cm.getClass().getName());
            @SuppressWarnings("unchecked")
            Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true); // Make the method callable
            // get the setting for "mobile data"
            mobileDataEnabled = (Boolean) method.invoke(cm);
        } catch (Exception e) {
            // Some problem accessible private API - no worries
        }
        boolean gpsEnabled = false;
        try {
            final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            gpsEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            //No worries
        }

        String resolution = null;
        try {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            resolution = Integer.toString(metrics.widthPixels) + "x" + Integer.toString(metrics.heightPixels);
        } catch (Exception e) {
            //No worries
        }

        mPhoneInfo = new Feedback.Phone(Build.MODEL,
                Build.VERSION.RELEASE,
                supplicantState,
                mobileDataEnabled,
                gpsEnabled,
                resolution);
    }

    private boolean validateForm(@NonNull View rootView) {
        if (mContent != null) {
            if (TextUtils.isEmpty(mContent.getText())) {
                if (mContentInputLayout != null) {
                    mContentInputLayout.setErrorEnabled(true);
                    mContentInputLayout.setError(getString(R.string.maoni_validate_must_not_be_blank));
                }
                return false;
            } else {
                if (mContentInputLayout != null) {
                    mContentInputLayout.setErrorEnabled(false);
                }
            }
        }
        //Call the validator implementation instead
        return mValidator == null || mValidator.validateForm(rootView);
    }

    @Override
    public final boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
        } else if (itemId == R.id.maoni_feedback_send) {
            validateAndSubmitForm();
        }
        return true;
    }

    private void validateAndSubmitForm() {
        //Validate form
        if (this.validateForm(mRootView)) {
            //Check that device is actually connected to the internet prior to going any further
            boolean includeScreenshot = false;
            if (mIncludeScreenshot != null) {
                includeScreenshot = mIncludeScreenshot.isChecked();
            }
            String contentText = "";
            if (mContent != null) {
                contentText = mContent.getText().toString();
            }

            //Call actual implementation
            final Feedback feedback =
                    new Feedback(mFeedbackUniqueId, mPhoneInfo, mAppInfo,
                            contentText, includeScreenshot, mScreenshotFilePath);
            if (mListener != null) {
                mListener.onSendButtonClicked(feedback);
            }
            finish();
        } //else do nothing - this is up to the implementation
    }

}
