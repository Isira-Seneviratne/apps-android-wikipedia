package org.wikipedia.activity

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.skydoves.balloon.Balloon
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.BreadcrumbsContextHelper
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.analytics.eventplatform.EventPlatformClient
import org.wikipedia.analytics.metricsplatform.MetricsPlatform
import org.wikipedia.appshortcuts.AppShortcuts
import org.wikipedia.auth.AccountUtil
import org.wikipedia.concurrency.FlowEventBus
import org.wikipedia.connectivity.ConnectionStateMonitor
import org.wikipedia.donate.DonateDialog
import org.wikipedia.events.LoggedOutInBackgroundEvent
import org.wikipedia.events.ReadingListsEnableDialogEvent
import org.wikipedia.events.ReadingListsNoLongerSyncedEvent
import org.wikipedia.events.SplitLargeListsEvent
import org.wikipedia.events.ThemeFontChangeEvent
import org.wikipedia.events.UnreadNotificationsEvent
import org.wikipedia.games.onthisday.OnThisDayGameResultFragment
import org.wikipedia.login.LoginActivity
import org.wikipedia.main.MainActivity
import org.wikipedia.notifications.NotificationPresenter
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.readinglist.ReadingListSyncBehaviorDialogs
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import org.wikipedia.readinglist.sync.ReadingListSyncEvent
import org.wikipedia.recurring.RecurringTasksExecutor
import org.wikipedia.richtext.CustomHtmlParser
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.Theme
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.ImageZoomHelper

abstract class BaseActivity : AppCompatActivity(), ConnectionStateMonitor.Callback {
    interface Callback {
        fun onPermissionResult(activity: BaseActivity, isGranted: Boolean)
    }
    private var currentTooltip: Balloon? = null
    private var imageZoomHelper: ImageZoomHelper? = null
    var callback: Callback? = null

    val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            callback?.onPermissionResult(this, isGranted)
    }

    private val requestDonateActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            ExclusiveBottomSheetPresenter.dismiss(supportFragmentManager)
            FeedbackUtil.showMessage(this, R.string.donate_gpay_success_message)
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // TODO: Show message(s) to the user if they deny the permission
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!DeviceUtil.assertAppContext(this, true)) {
            finish()
            return
        }

        setTheme()
        removeSplashBackground()

        if (AppShortcuts.ACTION_APP_SHORTCUT == intent.action) {
            intent.putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, Constants.InvokeSource.APP_SHORTCUTS)
            val shortcutId = intent.getStringExtra(AppShortcuts.APP_SHORTCUT_ID)
            if (!shortcutId.isNullOrEmpty()) {
                ShortcutManagerCompat.reportShortcutUsed(applicationContext, shortcutId)
            }
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Conditionally execute all recurring tasks
        RecurringTasksExecutor().run()
        if (Prefs.isReadingListsFirstTimeSync && AccountUtil.isLoggedIn) {
            Prefs.isReadingListsFirstTimeSync = false
            Prefs.isReadingListSyncEnabled = true
            ReadingListSyncAdapter.manualSyncWithForce()
        }

        WikipediaApp.instance.connectionStateMonitor.registerCallback(this)

        DeviceUtil.setLightSystemUiVisibility(this)
        setStatusBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color))
        setNavigationBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color))
        maybeShowLoggedOutInBackgroundDialog()

        Prefs.localClassName = localClassName

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                FlowEventBus.events.collectLatest { event ->
                    if (event is ThemeFontChangeEvent) {
                        ActivityCompat.recreate(this@BaseActivity)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                FlowEventBus.events.collectLatest { event ->
                    when (event) {
                        is SplitLargeListsEvent -> {
                            MaterialAlertDialogBuilder(this@BaseActivity)
                                .setMessage(getString(R.string.split_reading_list_message, Constants.MAX_READING_LIST_ARTICLE_LIMIT))
                                .setPositiveButton(R.string.reading_list_split_dialog_ok_button_text, null)
                                .show()
                        }
                        is ReadingListsNoLongerSyncedEvent -> {
                            ReadingListSyncBehaviorDialogs.detectedRemoteTornDownDialog(this@BaseActivity)
                        }
                        is ReadingListsEnableDialogEvent, (this@BaseActivity is MainActivity) -> {
                            ReadingListSyncBehaviorDialogs.promptEnableSyncDialog(this@BaseActivity)
                        }
                        is LoggedOutInBackgroundEvent -> {
                            maybeShowLoggedOutInBackgroundDialog()
                        }
                        is ReadingListSyncEvent -> {
                            if (event.showMessage && !Prefs.isSuggestedEditsHighestPriorityEnabled) {
                                FeedbackUtil.makeSnackbar(this@BaseActivity, getString(R.string.reading_list_toast_last_sync)).show()
                            }
                        }
                        is UnreadNotificationsEvent -> {
                            runOnUiThread {
                                if (!isDestroyed) {
                                    onUnreadNotification()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        WikipediaApp.instance.connectionStateMonitor.unregisterCallback(this)
        CustomHtmlParser.pruneBitmaps(this)
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        WikipediaApp.instance.appSessionEvent.persistSession()
        MetricsPlatform.client.onAppPause()
        EventPlatformClient.flushCachedEvents()
    }

    override fun onResume() {
        super.onResume()
        WikipediaApp.instance.appSessionEvent.touchSession()
        MetricsPlatform.client.onAppResume()
        BreadCrumbLogEvent.logScreenShown(this)
    }

    override fun onStart() {
        super.onStart()
        NotificationPresenter.maybeRequestPermission(this, notificationPermissionLauncher)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> false
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        BreadCrumbLogEvent.logBackPress(this)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN ||
                event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
            dismissCurrentTooltip()
        }

        BreadcrumbsContextHelper.dispatchTouchEvent(window, event)

        imageZoomHelper?.let {
            return it.onDispatchTouchEvent(event) || super.dispatchTouchEvent(event)
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data?.hasExtra(OnThisDayGameResultFragment.EXTRA_GAME_COMPLETED) == true) {
            OnThisDayGameResultFragment.maybeShowOnThisDayGameEndContent(this)
        }
    }

    protected fun setStatusBarColor(@ColorInt color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.statusBarColor = color
        }
    }

    protected fun setNavigationBarColor(@ColorInt color: Int) {
        DeviceUtil.setNavigationBarColor(window, color)
    }

    protected open fun setTheme() {
        if (WikipediaApp.instance.currentTheme != Theme.LIGHT) {
            setTheme(WikipediaApp.instance.currentTheme.resourceId)
        }
    }

    override fun onGoOffline() {}

    override fun onGoOnline() {}

    fun launchDonateDialog(campaignId: String? = null, donateUrl: String? = null) {
        ExclusiveBottomSheetPresenter.show(supportFragmentManager, DonateDialog.newInstance(campaignId, donateUrl))
    }

    fun launchDonateActivity(intent: Intent) {
        requestDonateActivity.launch(intent)
    }

    private fun removeSplashBackground() {
        window.setBackgroundDrawable(ColorDrawable(ResourceUtil.getThemedColor(this, R.attr.paper_color)))
    }

    private fun maybeShowLoggedOutInBackgroundDialog() {
        if (Prefs.loggedOutInBackground) {
            Prefs.loggedOutInBackground = false
            MaterialAlertDialogBuilder(this)
                    .setCancelable(false)
                    .setTitle(R.string.logged_out_in_background_title)
                    .setMessage(R.string.logged_out_in_background_dialog)
                    .setPositiveButton(R.string.logged_out_in_background_login) { _, _ -> startActivity(LoginActivity.newIntent(this@BaseActivity, LoginActivity.SOURCE_LOGOUT_BACKGROUND)) }
                    .setNegativeButton(R.string.logged_out_in_background_cancel, null)
                    .show()
        }
    }

    private fun dismissCurrentTooltip() {
        currentTooltip?.dismiss()
        currentTooltip = null
    }

    fun setCurrentTooltip(tooltip: Balloon) {
        dismissCurrentTooltip()
        currentTooltip = tooltip
    }

    fun setImageZoomHelper() {
        imageZoomHelper = ImageZoomHelper(this)
    }

    open fun onUnreadNotification() { }
}
