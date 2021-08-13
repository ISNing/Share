package org.exthmui.share.msnearshare;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import com.microsoft.connecteddevices.*;
import com.microsoft.connecteddevices.EventListener;
import com.microsoft.connecteddevices.remotesystems.commanding.RemoteSystemConnectionRequest;
import com.microsoft.connecteddevices.remotesystems.RemoteSystem;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemAddedEventArgs;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemAuthorizationKind;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemAuthorizationKindFilter;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemDiscoveryType;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemDiscoveryTypeFilter;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemFilter;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemRemovedEventArgs;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemStatusType;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemStatusTypeFilter;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemUpdatedEventArgs;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemWatcher;
import com.microsoft.connecteddevices.remotesystems.RemoteSystemWatcherErrorOccurredEventArgs;
import com.microsoft.connecteddevices.remotesystems.commanding.nearshare.*;
import android.net.Uri;
import org.exthmui.share.base.PeerInfo;
import org.exthmui.share.base.events.AcceptedOrRefusedEvent;
import org.exthmui.share.base.listeners.OnAcceptedOrRefusedListener;
import org.exthmui.share.base.listeners.OnProgressUpdatedListener;
import org.exthmui.share.controller.SenderInfo;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.logging.Level;

import static android.content.Intent.ACTION_SEND;
import static android.content.Intent.ACTION_SEND_MULTIPLE;

public class NearShareManager implements SenderInfo {

    private static final String TAG = "NearShareManager";

    private static final Class<? extends java.util.EventListener>[] LISTENER_TYPES_ALLOWED = {OnAcceptedOrRefusedListener.class, OnProgressUpdatedListener.class};

    private final Context mContext;
    private Collection<java.util.EventListener> mListeners;

    private ConnectedDevicesPlatform mPlatform;
    private RemoteSystemWatcher mRemoteSystemWatcher;
    private RemoteSystem mSelectedRemoteSystem;
    private NearShareSender mNearShareSender;
    private Uri[] mFiles;
    private boolean mWatcherStarted;

    public NearShareManager(Context context){
        this.mContext = context;
    }

    public void addListener(java.util.EventListener listener){
        if(mListeners == null) mListeners = new HashSet<>();
        for(Class<? extends java.util.EventListener> t:LISTENER_TYPES_ALLOWED){
            if(listener.getClass() = t){
                mListeners.add(listener);
                break;
            }
        }
    }

    private void notifyListeners(EventObject event) {
        for (java.util.EventListener listener:mListeners) {
            requestedTypes = listener. 
        }
        if(event instanceof AcceptedOrRefusedEvent){

        }
    }

    @Override
    public void initialize() {
        initializePlatform();
    }

    /**TODO:统一请求权限
     * Request COARSE_LOCATION permission required for nearshare functionality over bluetooth.
     */
    private void requestPermissions() {
        // Request user permission for app to use location services, which is a requirement for Bluetooth.
        Random rng = new Random();
        int permissionRequestCode = rng.nextInt(128);

        int permissionCheck =
                ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(
                    this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, permissionRequestCode);
        } else {
            Log.d(TAG, "Requested User Permission To Enable NearShare Prerequisites");
        }
    }

    /**
     * Initialize the platform. This is required before we attempt to use CDP SDK.
     * Steps to start platform:
     * 1. Initialize platform
     * 2. Request Access Token
     * 3. Start Platform
     */
    private void initializePlatform() {
        mPlatform = new ConnectedDevicesPlatform(mContext);

        // Subscribe to NotificationRegistrationStateChanged event
        mPlatform.getNotificationRegistrationManager().notificationRegistrationStateChanged().subscribe((notificationRegistrationManager, args) -> onNotificationRegistrationStateChanged(notificationRegistrationManager, args));
        mPlatform.start();

        // After platform start, before we can start remotesystem discovery, need to addaccount,
        // NearShare only requires anonymous account, other CDP scenarios may require adding signed in
        // accounts.
        createAndAddAnonymousAccount(mPlatform);
    }

    /**
     * NearShare just works with anonymous account, signed in accounts are needed when using other CDP
     * features.
     */
    private void createAndAddAnonymousAccount(ConnectedDevicesPlatform platform) {
        ConnectedDevicesAccount account = ConnectedDevicesAccount.getAnonymousAccount();
        platform.getAccountManager().addAccountAsync(account).whenComplete((ConnectedDevicesAddAccountResult result, Throwable throwable) -> {
            if (throwable != null) {
                Log.d(TAG, String.format("AccountManager addAccountAsync returned a throwable: %1$s", throwable.getMessage()));
            } else {
                Log.d(TAG, "AccountManager : Added account successfully");
            }
        });
    }

    /**
     * Event for when the registration state changes for a given account.
     *
     * @param sender ConnectedDevicesNotificationRegistrationManager which is making the request
     * @param args   Contains arguments for the event
     */
    private void onNotificationRegistrationStateChanged(ConnectedDevicesNotificationRegistrationManager sender, ConnectedDevicesNotificationRegistrationStateChangedEventArgs args) {
        Log.i(TAG, "NotificationRegistrationStateChanged for account");
    }

    /**
     * This method starts the RemoteSystem discovery process. It sets the corresponding filters
     * to ensure that only spatially proximal devices are listed. It also sets up listeners
     * for important events, such as device added, device updated, and device removed
     */
    private void startRemoteSystemWatcher() {
        ArrayList<RemoteSystemFilter> filters = new ArrayList<>();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean proximalMode = preferences.getBoolean("proximal_mode" , false);
        if (proximalMode) {
            filters.add(new RemoteSystemDiscoveryTypeFilter(RemoteSystemDiscoveryType.PROXIMAL));
        } else {
            filters.add(new RemoteSystemDiscoveryTypeFilter(RemoteSystemDiscoveryType.SPATIALLY_PROXIMAL));
        }

        filters.add(new RemoteSystemStatusTypeFilter(RemoteSystemStatusType.ANY));
        filters.add(new RemoteSystemAuthorizationKindFilter(RemoteSystemAuthorizationKind.ANONYMOUS));

        mRemoteSystemWatcher = new RemoteSystemWatcher(filters);
        final WeakReference<RemoteSystemWatcher> weakRemoteSystemWatcher = new WeakReference<>(mRemoteSystemWatcher);
        weakRemoteSystemWatcher.get().remoteSystemAdded().subscribe(new RemoteSystemAddedListener());
        weakRemoteSystemWatcher.get().remoteSystemUpdated().subscribe(new RemoteSystemUpdatedListener());
        weakRemoteSystemWatcher.get().remoteSystemRemoved().subscribe(new RemoteSystemRemovedListener());
        weakRemoteSystemWatcher.get().errorOccurred().subscribe(new RemoteSystemWatcherErrorOccurredListener());

        // Everytime user toggles checkbox Proximal discovery
        // we restart the watcher with appropriate filters to whiter do a
        // Proximal or Spatially Proximal discovery. this check is to see if watcher has been previously started
        // if was started, we stop it and restart with the new set of filters
        if (mWatcherStarted) {
            weakRemoteSystemWatcher.get().stop();
            mWatcherStarted = false;
            mRemoteDeviceListAdapter.clear();
            mRemoteDeviceListAdapter.notifyDataSetChanged();
        }

        weakRemoteSystemWatcher.get().start();
        mWatcherStarted = true;
    }

    /**
     * Helper Function to initialize the files based on whether user is trying to share
     * single file or multiple files.
     */
    private void initFiles() {
        Intent launchIntent = getIntent();

        if ((ACTION_SEND == launchIntent.getAction()) || (ACTION_SEND_MULTIPLE == launchIntent.getAction())) {
            switch (launchIntent.getAction()) {
                case ACTION_SEND: {
                    mFiles = new Uri[]{launchIntent.getParcelableExtra(Intent.EXTRA_STREAM)};
                    break;
                }
                case ACTION_SEND_MULTIPLE: {
                    ArrayList<Uri> files = launchIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                    mFiles = new Uri[files.size()];
                    files.toArray(mFiles);
                    break;
                }
            }
        }
    }

    /**
     * Send URI to the target device using nearshare
     */
    private void sendUri(PeerInfo t, String uriText) {
            RemoteSystemConnectionRequest remoteSystemConnectionRequest = new RemoteSystemConnectionRequest(mSelectedRemoteSystem);

            if (mNearShareSender.isNearShareSupported(remoteSystemConnectionRequest)) {
                mNearShareSender.sendUriAsync(remoteSystemConnectionRequest, uriText);
            }
            else
            {
                Log.d(TAG, "NearShare is not supported in this device");
            }
    }

    /**
     * Pick Files and Send using NearShare, helper function to pick files and send.
     */
    private AsyncOperationWithProgress<NearShareStatus, NearShareProgress> setupAndBeginSendFileAsync() {
        AsyncOperationWithProgress<NearShareStatus, NearShareProgress> asyncFileTransferOperation = null;
        asyncFileTransferOperation.progress().subscribe((op, progress) -> {
            if (progress.filesSent != 0 || progress.totalFilesToSend != 0) {
                if (accepted.compareAndSet(false, true)) {
                    mHandler.post(listener::onAccepted);
                }
                mHandler.post() -> listener.onProgress(progress.bytesSent, progress.totalBytesToSend));
            }
        })
        if ((null != mFiles) && (null != mSelectedRemoteSystem)) {
            RemoteSystemConnectionRequest remoteSystemConnectionRequest = new RemoteSystemConnectionRequest(mSelectedRemoteSystem);

            if (mNearShareSender.isNearShareSupported(remoteSystemConnectionRequest)) {

                CancellationToken cancellationToken = null;

                findViewById(R.id.btnCancel).setEnabled(true);

                // Call the appropriate api based on the number of files shared to the app.
                if (1 == mFiles.length) {
                    NearShareFileProvider nearShareFileProvider =
                            NearShareHelper.createNearShareFileFromContentUri(mFiles[0], mContext);

                    asyncFileTransferOperation =
                            mNearShareSender.sendFileAsync(remoteSystemConnectionRequest, nearShareFileProvider);
                } else {
                    NearShareFileProvider[] nearShareFileProviderArray = new NearShareFileProvider[mFiles.length];

                    for (int index = 0; index < mFiles.length; ++index) {
                        nearShareFileProviderArray[index] =
                                NearShareHelper.createNearShareFileFromContentUri(mFiles[index], mContext);
                    }

                    asyncFileTransferOperation =
                            mNearShareSender.sendFilesAsync(remoteSystemConnectionRequest, nearShareFileProviderArray);

                }
            }
        }
        return asyncFileTransferOperation;
    }

    /**
     * Send file(s) to the target device using nearshare. Select a photo or file and share to the nearshare app.
     */
    private void sendFile() {
        AsyncOperation<NearShareStatus> asyncFileTransferOperation = setupAndBeginSendFileAsync();
        if ((null != mFiles) && (null != mSelectedRemoteSystem)) {
            ((Button) findViewById(R.id.btnCancel))
                    .setOnClickListener(new View.OnClickListener() {
                        private AsyncOperation<NearShareStatus> mAsyncOperation;

                        private View.OnClickListener init(AsyncOperation<NearShareStatus> asyncOperation) {
                            mAsyncOperation = asyncOperation;
                            return this;
                        }

                        /**
                         * Called when a view has been clicked.
                         *
                         * @param v The view that was clicked.
                         */
                        @Override
                        public void onClick(View v) {
                            mAsyncOperation.cancel(true);
                        }
                    }.init(asyncFileTransferOperation));

            asyncFileTransferOperation.whenCompleteAsync(new AsyncOperation.ResultBiConsumer<NearShareStatus, Throwable>() {
                @Override
                public void accept(NearShareStatus nearShareStatus, Throwable throwable) throws Throwable {
                    findViewById(R.id.btnCancel).setEnabled(false);
                    if (null != throwable) {
                        LOG.log(Level.SEVERE, String.format("Exception during file transfer: %1$s", throwable.getMessage()));
                    } else {
                        if (nearShareStatus == NearShareStatus.COMPLETED) {
                            LOG.log(Level.INFO, "File transfer completed");
                        } else {
                            LOG.log(Level.SEVERE, "File transfer failed");
                        }
                    }
                }
            });
        }
    }

    /**
     * Callback method to be invoked when an item in this AdapterView has
     * been clicked.
     * <p>
     * Implementers can call getItemAtPosition(position) if they need
     * to access the data associated with the selected item.
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this
     *                 will be a view provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id       The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mSelectedRemoteSystem = (RemoteSystem)mRemoteDeviceListAdapter.getItem(position);
        mRemoteDeviceListAdapter.setSelectedView(view);
    }

    @Override
    public boolean isAvailable() {
        //TODO:Add test on feature dependencies
        return true;
    }

    // region HelperClasses
    private static class RemoteSystemAddedListener implements EventListener<RemoteSystemWatcher, RemoteSystemAddedEventArgs> {
        @Override
        public void onEvent(RemoteSystemWatcher remoteSystemWatcher, RemoteSystemAddedEventArgs args) {
            final RemoteSystem remoteSystemParam = args.getRemoteSystem();
            // Calls from the OneSDK are not guaranteed to come back on the given (UI) thread
            // hence explicitly call runOnUiThread
                    mRemoteDeviceListAdapter.addDevice(remoteSystemParam);
                    mRemoteDeviceListAdapter.notifyDataSetChanged();
        }
    }

    private static class RemoteSystemUpdatedListener implements EventListener<RemoteSystemWatcher, RemoteSystemUpdatedEventArgs> {
        @Override
        public void onEvent(RemoteSystemWatcher remoteSystemWatcher, RemoteSystemUpdatedEventArgs args) {
            LOG.log(Level.INFO, String.format("Updating system: %1$s", args.getRemoteSystem().getDisplayName()));
        }
    }

    private static class RemoteSystemRemovedListener implements EventListener<RemoteSystemWatcher, RemoteSystemRemovedEventArgs> {
        @Override
        public void onEvent(RemoteSystemWatcher remoteSystemWatcher, RemoteSystemRemovedEventArgs args) {
            final RemoteSystem remoteSystemParam = args.getRemoteSystem();
            // Calls from the OneSDK are not guaranteed to come back on the given (UI) thread
            // hence explicitly call runOnUiThread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mRemoteDeviceListAdapter.removeDevice(remoteSystemParam);
                    mRemoteDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    private static class RemoteSystemWatcherErrorOccurredListener implements EventListener<RemoteSystemWatcher, RemoteSystemWatcherErrorOccurredEventArgs> {
        @Override
        public void onEvent(RemoteSystemWatcher remoteSystemWatcher, RemoteSystemWatcherErrorOccurredEventArgs args) {
            LOG.log(Level.SEVERE, String.format("Discovery error: %1$s", args.getError().toString()));
        }
    }
    // endregion HelperClasses
}
