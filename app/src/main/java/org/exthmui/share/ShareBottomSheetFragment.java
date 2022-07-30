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

import org.exthmui.share.misc.SendingHelper;
import org.exthmui.share.services.DiscoverService;
import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.IPeer;
import org.exthmui.share.shared.exceptions.NoEntityPassedException;
import org.exthmui.share.shared.exceptions.PeerDisappearedException;
import org.exthmui.share.shared.listeners.BaseEventListener;
import org.exthmui.share.shared.listeners.OnDiscovererStartedListener;
import org.exthmui.share.shared.listeners.OnDiscovererStoppedListener;
import org.exthmui.share.shared.listeners.OnPeerAddedListener;
import org.exthmui.share.shared.listeners.OnPeerRemovedListener;
import org.exthmui.share.shared.listeners.OnPeerUpdatedListener;
import org.exthmui.share.shared.misc.ServiceUtils;
import org.exthmui.share.shared.misc.StackTraceUtils;
import org.exthmui.share.shared.ui.BaseBottomSheetFragment;
import org.exthmui.share.ui.PeerChooserView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ShareBottomSheetFragment extends BaseBottomSheetFragment {

    public static final String TAG = "ShareBottomSheetFragment";

    private static final String KEY_ENTITIES_SAVED = "URIS_SAVED";

    public void setEntities(ArrayList<Entity> entities) {
        if (mEntities == null) {
            return;
        }
        mEntities = entities;

        if (mTitle != null) {
            if (mEntities.size() == 1) {
                mTitle.setText(getString(R.string.title_send_file, mEntities.get(0).getFileName()));
            } else {
                mTitle.setText(getString(R.string.title_send_files, mEntities.get(0).getFileName(),
                        mEntities.size() - 1));
            }
        }
    }

    private final ServiceUtils.MyServiceConnection mConnection = new ServiceUtils.MyServiceConnection();
    @Nullable
    private DiscoverService mService;

    private final Set<BaseEventListener> mServiceListeners = new HashSet<>();

    private PeerChooserView mPeerChooser;
    private Button mCancelButton;
    private TextView mTitle;
    private Button mActionButton;
    private SendingHelper mSendingHelper;

    private ArrayList<Entity> mEntities = new ArrayList<>();

    public ShareBottomSheetFragment() {
    }

    private void initListeners() {

        mConnection.registerOnServiceConnectedListener(service -> {
            mService = (DiscoverService) service;
            if (mService.isAnyDiscovererStarted()) {
                Map<String, IPeer> peers = mService.getPeerInfoMap();
                mPeerChooser.setData(peers);
            } else if (!mService.isDiscoverersAvailable()) {
                mPeerChooser.setState(PeerChooserView.STATE_UNAVAILABLE);
            } else {
                mPeerChooser.setState(PeerChooserView.STATE_DISABLED);
            }

            mServiceListeners.add((OnDiscovererStartedListener) event -> {
                int state = mPeerChooser.getState();
                if (state != PeerChooserView.STATE_ENABLED &&
                        state != PeerChooserView.STATE_ENABLED_NO_PEER) {
                    Map<String, IPeer> peers = mService.getPeerInfoMap();
                    requireActivity().runOnUiThread(() -> mPeerChooser.setData(peers));
                }
            });
            mServiceListeners.add((OnDiscovererStoppedListener) event -> {
                if (!mService.isDiscoverersAvailable()) {
                    mPeerChooser.setState(PeerChooserView.STATE_UNAVAILABLE);
                } else {
                    mPeerChooser.setState(PeerChooserView.STATE_DISABLED);
                }
            });
            mServiceListeners.add((OnPeerAddedListener) event -> requireActivity().runOnUiThread(() -> mPeerChooser.addPeer(event.getPeer())));
            mServiceListeners.add((OnPeerUpdatedListener) event -> requireActivity().runOnUiThread(() -> mPeerChooser.updatePeer(event.getPeer())));
            mServiceListeners.add((OnPeerRemovedListener) event -> requireActivity().runOnUiThread(() -> mPeerChooser.removePeer(event.getPeer())));
            for (BaseEventListener listener : mServiceListeners) {
                mService.registerListener(listener);
            }

            mPeerChooser.setEnableButtonOnClickListener(var1 -> mService.startDiscoverers());
        });
        mConnection.registerOnServiceDisconnectedListener(name -> mService = null);
        requireContext()
                .bindService(new Intent(requireContext(), DiscoverService.class), mConnection,
                        BIND_AUTO_CREATE);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mEntities = savedInstanceState.getParcelableArrayList(KEY_ENTITIES_SAVED);
        }

        initListeners();

        mSendingHelper = new SendingHelper(requireContext());
    }

    @Override
    public void onPause() {
        unbindServices();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        initListeners();
    }

    @Override
    public void onDestroy() {
        unbindServices();
        super.onDestroy();
    }

    private void unbindServices(){
        if (mService != null) {
            for (BaseEventListener listener : mServiceListeners) {
                mService.unregisterListener(listener);
            }
            mService.beforeUnbind();
            requireContext().unbindService(mConnection);
            mService = null;
        }
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

        mCancelButton.setOnClickListener(v -> cancel());

        if (!mEntities.isEmpty()) {
            setEntities(mEntities);
        }

        mPeerChooser.getPeerSelectedLiveData()
                .observe(this, s -> mActionButton.setClickable(s != null));
        mActionButton.setOnClickListener(v -> {
            if (mEntities == null) {
                throw new NoEntityPassedException();
            }
            if (mService == null) {
                mConnection.registerOnServiceConnectedListener(s -> {
                    DiscoverService service = (DiscoverService) s;
                    IPeer peer = service.getPeerInfoMap().get(mPeerChooser.getPeerSelected());
                    try {
                        if (peer == null) {
                            throw new PeerDisappearedException(requireContext());
                        }
                        mSendingHelper.send(peer, mEntities);
                    } catch (Throwable tr) {
                        handleError(requireContext(), tr);
                    }
                    dismiss();
                });
            } else {
                IPeer peer = mService.getPeerInfoMap().get(mPeerChooser.getPeerSelected());
                try {
                    if (peer == null) {
                        throw new PeerDisappearedException(requireContext());
                    }
                    mSendingHelper.send(peer, mEntities);
                } catch (Throwable tr) {
                    handleError(requireContext(), tr);
                }
                dismiss();
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelableArrayList(KEY_ENTITIES_SAVED, mEntities);
        super.onSaveInstanceState(outState);
    }

    private void handleError(Context context, String message, String localizedMessage) {
        Toast.makeText(context, localizedMessage, Toast.LENGTH_SHORT).show();
        Log.e(TAG, message);
    }

    private void handleError(@NonNull Context context, @NonNull Throwable throwable) {
        handleError(context, throwable.getMessage(), throwable.getLocalizedMessage());
        Log.e(TAG, StackTraceUtils.getStackTraceString(throwable.getStackTrace()));
    }
}