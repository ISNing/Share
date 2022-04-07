package org.exthmui.share;

import static android.content.Context.BIND_AUTO_CREATE;

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

    public static final String TAG = "ShareBottomSheetFragment";

    public void setEntities(List<Entity> entities) {
        if (mEntities != null) mEntities = entities;
    }

    private final ServiceUtils.MyServiceConnection mConnection = new ServiceUtils.MyServiceConnection();
    @Nullable
    private DiscoverService mService;

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
                int state = mPeerChooser.getState();
                if (state != PeerChooserView.STATE_ENABLED &&
                        state != PeerChooserView.STATE_ENABLED_NO_PEER) {
                    Map<String, PeerInfo> peers = mService.getPeerInfoMap();
                    requireActivity().runOnUiThread(() -> mPeerChooser.setData(peers));
                }
            });
            mServiceListeners.add((OnDiscovererStoppedListener) event -> {
                if (!mService.isDiscoverersAvailable())
                    mPeerChooser.setState(PeerChooserView.STATE_UNAVAILABLE);
                else mPeerChooser.setState(PeerChooserView.STATE_DISABLED);
            });
            mServiceListeners.add((OnPeerAddedListener) event -> {
                requireActivity().runOnUiThread(() -> mPeerChooser.addPeer(event.getPeer()));
            });
            mServiceListeners.add((OnPeerUpdatedListener) event -> {
                requireActivity().runOnUiThread(() -> mPeerChooser.updatePeer(event.getPeer()));
            });
            mServiceListeners.add((OnPeerRemovedListener) event -> {
                requireActivity().runOnUiThread(() -> mPeerChooser.removePeer(event.getPeer()));
            });
            for (BaseEventListener listener : mServiceListeners)
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
            for (BaseEventListener listener : mServiceListeners)
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
        mPeerChooser.initialize(getChildFragmentManager());
        mTitle = view.findViewById(R.id.fragment_share_title);
        mCancelButton = view.findViewById(R.id.fragment_share_cancel_button);
        mActionButton = view.findViewById(R.id.fragment_share_action_button);

        mCancelButton.setOnClickListener(v -> onCancel());

        if (mEntities.isEmpty()) {
            Log.w(TAG, "No file was selected");
            Toast.makeText(getContext(), R.string.toast_no_file, Toast.LENGTH_SHORT).show();
            dismiss();
        } else if (mEntities.size() == 1) {
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

            public void sendTo(@NonNull PeerInfo target, @NonNull List<Entity> entities) {
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
                    dismiss();
                });
                else {
                    PeerInfo peer = mService.getPeerInfoMap().get(mPeerChooser.getPeerSelected());
                    if (peer == null) {
                        handleError(requireContext(), "Peer disappeared");// TODO:handle failure
                        return;
                    }
                    if (mEntities.size() == 1) {
                        sendTo(peer, mEntities.get(0));
                    } else {
                        sendTo(peer, mEntities);
                    }
                    dismiss();
                }
            }
        });
    }

    private void handleError(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public void onCancel() {
        dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        requireActivity().finish();
    }
}