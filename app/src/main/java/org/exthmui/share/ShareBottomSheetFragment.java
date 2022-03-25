package org.exthmui.share;

import static android.content.Context.BIND_AUTO_CREATE;
import static org.exthmui.share.PeersAdapter.REQUEST_CODE_PICK_FILE;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.exthmui.share.misc.Constants;
import org.exthmui.share.services.DiscoverService;
import org.exthmui.share.shared.ServiceUtils;
import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.PeerInfo;
import org.exthmui.share.shared.base.Sender;
import org.exthmui.share.shared.base.exceptions.NoEntityPassedException;
import org.exthmui.share.shared.base.listeners.BaseEventListener;
import org.exthmui.share.shared.base.listeners.OnDiscovererStartedListener;
import org.exthmui.share.shared.base.listeners.OnDiscovererStoppedListener;
import org.exthmui.share.shared.base.listeners.OnPeerAddedListener;
import org.exthmui.share.shared.base.listeners.OnPeerRemovedListener;
import org.exthmui.share.shared.base.listeners.OnPeerUpdatedListener;
import org.exthmui.share.ui.PeerChooserView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ShareBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String TAG = "ShareBottomSheetFragment";

    public void setEntities(List<Entity> entities) {
        if(mEntities != null) mEntities = entities;
    }

    public enum ClickAction{
        SEND_ENTITIES,
        CHOOSE_ENTITIES
    }

    private ClickAction CLICK_ACTION;

    private final ServiceUtils.MyServiceConnection mConnection = new ServiceUtils.MyServiceConnection();
    private @Nullable DiscoverService mService;

    private final Set<BaseEventListener> mServiceListeners = new HashSet<>();
    
    private PeerChooserView mPeerChooser;
    private Button mCancelButton;
    private TextView mTitle;
    private Button mActionButton;

    private List<Entity> mEntities = new ArrayList<>();

//    private final WifiStateMonitor mWifiStateMonitor = new WifiStateMonitor() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            setupIfNeeded();
//        }
//    };
//
//    private final BluetoothStateMonitor mBluetoothStateMonitor = new BluetoothStateMonitor() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            setupIfNeeded();
//        }
//    };

    public ShareBottomSheetFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mConnection.registerOnServiceConnectedListener(service -> {
            mService = (DiscoverService) service;
            if (mService.isAnyDiscovererStarted()) {
                Map<String, PeerInfo> peers = mService.getPeerInfoMap();
                mPeerChooser.setData(peers);
            } else if (!mService.isDiscoverersAvailable())
                mPeerChooser.setState(PeerChooserView.STATE_UNAVAILABLE);
            else mPeerChooser.setState(PeerChooserView.STATE_DISABLED);

            mServiceListeners.add((OnDiscovererStartedListener) event -> {
                mPeerChooser.discoverStarted();
            });
            mServiceListeners.add((OnDiscovererStoppedListener) event -> {
                if (!mService.isDiscoverersAvailable())
                    mPeerChooser.setState(PeerChooserView.STATE_UNAVAILABLE);
                else mPeerChooser.setState(PeerChooserView.STATE_DISABLED);
            });
            mServiceListeners.add((OnPeerAddedListener) event -> {
                mPeerChooser.addPeer(event.getPeer());
            });
            mServiceListeners.add((OnPeerUpdatedListener) event -> {
                mPeerChooser.updatePeer(event.getPeer());
            });
            mServiceListeners.add((OnPeerRemovedListener) event -> {
                mPeerChooser.removePeer(event.getPeer());
            });
            for (BaseEventListener listener: mServiceListeners)
                mService.registerListener(listener);

            mPeerChooser.setEnableButtonOnClickListener(var1 -> mService.startDiscoverers());
        });
        mConnection.registerOnServiceDisconnectedListener(name -> mService = null);
        requireContext().bindService(new Intent(requireContext(), DiscoverService.class), mConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            for (BaseEventListener listener: mServiceListeners)
                mService.unregisterListener(listener);
            mService.beforeUnbind();
        }
        requireContext().unbindService(mConnection);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_share, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPeerChooser = view.findViewById(R.id.fragment_share_peer_chooser);
        mTitle = view.findViewById(R.id.fragment_share_title);
        mCancelButton = view.findViewById(R.id.fragment_share_cancel_button);
        mActionButton = view.findViewById(R.id.fragment_share_action_button);

        mCancelButton.setOnClickListener(v -> onCancel());

        if(mEntities.isEmpty()) {
            Log.w(TAG, "No file was selected");
            Toast.makeText(getContext(), R.string.toast_no_file, Toast.LENGTH_SHORT).show();
            requireActivity().finish();
        } else if(mEntities.size() == 1) {
            mTitle.setText(getString(R.string.title_send_file, mEntities.get(0).getFileName()));
        } else {
            mTitle.setText(getString(R.string.title_send_files, mEntities.get(0).getFileName(), mEntities.size() - 1));
        }

        mPeerChooser.getPeerSelectedLiveData().observe(this, s -> mActionButton.setClickable(s != null));
        mActionButton.setOnClickListener(new View.OnClickListener() {
            public void sendTo(@NonNull PeerInfo target, @NonNull Entity entity) {
                Constants.ConnectionType connectionType = Constants.ConnectionType.parseFromCode(target.getConnectionType());
                if (connectionType == null) return;// TODO: handle failure
                try {
                    Method method = connectionType.getSenderClass().getDeclaredMethod("getInstance", Context.class);
                    Sender<?> sender = (Sender<?>) method.invoke(null, requireContext());
                    if (sender == null) return;// TODO: handle failure
                    sender.sendToPeerInfo(target, entity);
                } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
                    exception.printStackTrace();
                }
            }

            public void sendTo(@NonNull PeerInfo target, @NonNull List<Entity> entities){
                Constants.ConnectionType connectionType = Constants.ConnectionType.parseFromCode(target.getConnectionType());
                if (connectionType == null) return;// TODO: handle failure
                try {
                    Method method = connectionType.getSenderClass().getDeclaredMethod("getInstance", Context.class);
                    Sender<?> sender = (Sender<?>) method.invoke(null, requireContext());
                    if (sender == null) return;// TODO: handle failure
                    sender.sendToPeerInfo(target, entities);
                } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
                    exception.printStackTrace();
                }
            }
            @Override
            public void onClick(View v) {
                if (CLICK_ACTION == ClickAction.CHOOSE_ENTITIES) {//TODO: remove it?
                    Intent requestIntent = new Intent(Intent.ACTION_GET_CONTENT)
                            .addCategory(Intent.CATEGORY_OPENABLE)
                            .setType("*/*")
                            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    ActivityCompat.startActivityForResult(requireActivity(), Intent.createChooser(requestIntent, "File"), REQUEST_CODE_PICK_FILE, null);
                } else if (CLICK_ACTION == ClickAction.SEND_ENTITIES) {
                    if (mEntities == null) throw new NoEntityPassedException();
                    if (mService == null) mConnection.registerOnServiceConnectedListener(s -> {
                        DiscoverService service = (DiscoverService) s;
                        PeerInfo peer = service.getPeerInfoMap().get(mPeerChooser.getPeerSelected());
                        if (peer == null) return; // TODO: handle failure
                        if (mEntities.size() == 1) {
                            sendTo(peer, mEntities.get(0));
                        } else {
                            sendTo(peer, mEntities);
                        }
                    });
                    else {
                        PeerInfo peer = mService.getPeerInfoMap().get(mPeerChooser.getPeerSelected());
                        if (peer == null) return; // TODO: handle failure
                        if (mEntities.size() == 1) {
                            sendTo(peer, mEntities.get(0));
                        } else {
                            sendTo(peer, mEntities);
                        }
                    }
                }
            }
        });
    }

//    @Override
//    public void onResume() {
//        super.onResume();
//
//        mWifiStateMonitor.register(getContext());
//        mBluetoothStateMonitor.register(getContext());
//
//        if (setupIfNeeded()) {
//            return;
//        }
//
//        if (!mIsDiscovering) {
//            mAirDropManager.startDiscover(this);
//            mNearShareManager.startDiscover(this);
//            mIsDiscovering = true;
//        }
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//
//        if (mIsDiscovering && !mShouldKeepDiscovering) {
//            mAirDropManager.stopDiscover();
//            mNearShareManager.stopDiscover();
//            mIsDiscovering = false;
//        }
//
//        mWifiStateMonitor.unregister(getContext());
//        mBluetoothStateMonitor.unregister(getContext());
//    }
//

    public void onCancel() {
        requireActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        requireActivity().finish();
    }
}