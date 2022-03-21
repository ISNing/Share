package org.exthmui.share;

import static android.content.Context.BIND_AUTO_CREATE;
import static org.exthmui.share.PeersAdapter.REQUEST_CODE_PICK_FILE;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.exthmui.share.databinding.FragmentShareBinding;
import org.exthmui.share.services.DiscoverService;
import org.exthmui.share.shared.ServiceUtils;
import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.PeerInfo;
import org.exthmui.share.shared.base.Sender;
import org.exthmui.share.shared.base.exceptions.NoEntityPassedException;
import org.exthmui.share.shared.base.listeners.BaseEventListener;
import org.exthmui.share.shared.base.listeners.OnPeerAddedListener;
import org.exthmui.share.shared.base.listeners.OnPeerRemovedListener;
import org.exthmui.share.shared.base.listeners.OnPeerUpdatedListener;

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

    ServiceUtils.MyServiceConnection mConnection = new ServiceUtils.MyServiceConnection();
    @Nullable DiscoverService mService;

    FragmentShareBinding binding;

    private List<Entity> mEntities = new ArrayList<>();

    private ShareActivity mParent;

    private PeersAdapter mAdapter;

//    private PartialWakeLock mWakeLock;

    private final boolean mIsInSetup = false;

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

    private final boolean mIsDiscovering = false;
    private final boolean mShouldKeepDiscovering = false;

//    private SendingSession mSending;

    public ShareBottomSheetFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        mWakeLock = new PartialWakeLock(getContext(), TAG);
//        mAirDropManager = new AirDropManager(getContext(),
//                WarpShareApplication.from(getContext()).getCertificateManager());
        mAdapter = new PeersAdapter(getContext());
        mConnection.registerOnServiceConnectedListener(service -> {
            mService = (DiscoverService) service;
            Map<String, PeerInfo> peers = mService.getPeerInfoMap();
            mAdapter.setData(peers);
            if (!peers.isEmpty()) {
                ConstraintLayout placeholderContainer = binding.scanningPlaceholder.scanningPlaceholderContainer;
                if (placeholderContainer.getVisibility() == View.VISIBLE)
                    placeholderContainer.setVisibility(View.GONE);
                if (binding.sharePeers.getVisibility() == View.GONE)
                    binding.sharePeers.setVisibility(View.VISIBLE);
            }
            Set<BaseEventListener> listeners = new HashSet<>();
            listeners.add((OnPeerAddedListener) event -> {
                mAdapter.addPeer(event.getPeer());
                ConstraintLayout placeholderContainer = binding.scanningPlaceholder.scanningPlaceholderContainer;
                if (placeholderContainer.getVisibility() == View.VISIBLE)
                    placeholderContainer.setVisibility(View.GONE);
                if (binding.sharePeers.getVisibility() == View.GONE)
                    binding.sharePeers.setVisibility(View.VISIBLE);
            });
            listeners.add((OnPeerUpdatedListener) event -> mAdapter.updatePeer(event.getPeer()));
            listeners.add((OnPeerRemovedListener) event -> {
                mAdapter.removePeer(event.getPeer());
                if (peers.isEmpty()) {
                    ConstraintLayout placeholderContainer = binding.scanningPlaceholder.scanningPlaceholderContainer;
                    if (placeholderContainer.getVisibility() == View.GONE)
                        placeholderContainer.setVisibility(View.VISIBLE);
                    if (binding.sharePeers.getVisibility() == View.VISIBLE)
                        binding.sharePeers.setVisibility(View.GONE);
                }
            });
            mService.registerDiscoverersListeners(listeners);
        });
        requireContext().bindService(new Intent(requireContext(), DiscoverService.class), mConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        requireContext().unbindService(mConnection);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mParent = (ShareActivity) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentShareBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final RecyclerView peersView = view.findViewById(R.id.share_peers);
        peersView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        peersView.setAdapter(mAdapter);
        binding.shareCancel.setOnClickListener(v -> onCancel());
        binding.scanningPlaceholder.waveView.setWaveColor(Color.parseColor("#DB4437"));
        binding.scanningPlaceholder.waveView.startWave();

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        mAdapter.setEntities(mEntities);
        binding.sharePeers.setLayoutManager(layoutManager);
        binding.sharePeers.setAdapter(mAdapter);

        if(mEntities.isEmpty()) {
            Log.w(TAG, "No file was selected");
            Toast.makeText(getContext(), R.string.toast_no_file, Toast.LENGTH_SHORT).show();
            mParent.finish();
        } else if(mEntities.size() == 1) {
            binding.shareTitle.setText(String.format(getString(R.string.title_send_file), mEntities.get(0).getFileName()));
        } else {
            binding.shareTitle.setText(String.format(getString(R.string.title_send_files), mEntities.get(0).getFileName(), mEntities.size() - 1));
        }

        mAdapter.getPeerSelectedLiveData().observe(this, s -> binding.shareAction.setClickable(s != null));
        binding.shareAction.setOnClickListener(new View.OnClickListener() {
            public void sendTo(@NonNull PeerInfo target, @NonNull Entity entity) {
                org.exthmui.share.misc.Constants.ConnectionType connectionType = org.exthmui.share.misc.Constants.ConnectionType.parseFromCode(target.getConnectionType());
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
                org.exthmui.share.misc.Constants.ConnectionType connectionType = org.exthmui.share.misc.Constants.ConnectionType.parseFromCode(target.getConnectionType());
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
                    Intent requestIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    requestIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    requestIntent.setType("*/*");
                    requestIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    ActivityCompat.startActivityForResult(requireActivity(), Intent.createChooser(requestIntent, "File"), REQUEST_CODE_PICK_FILE, null);
                } else if (CLICK_ACTION == ClickAction.SEND_ENTITIES) {
                    if (mEntities == null) throw new NoEntityPassedException();
                    if (mService == null) mConnection.registerOnServiceConnectedListener(s -> {
                        DiscoverService service = (DiscoverService) s;
                        PeerInfo peer = service.getPeerInfoMap().get(mAdapter.getPeerSelected());
                        if (peer == null) return; // TODO: handle failure
                        sendTo(peer, mEntities);
                    });
                    else {
                        PeerInfo peer = mService.getPeerInfoMap().get(mAdapter.getPeerSelected());
                        if (peer == null) return; // TODO: handle failure
                        sendTo(peer, mEntities);
                    }
                }
            }
        });
//        mSendButton.setOnClickListener(v -> sendFile(mPeers.get(mPeerPicked), mEntities));
//
//        mDiscoveringView = view.findViewById(R.id.discovering);
//
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
//    @Override
//    public void onDismiss(DialogInterface dialog) {
//        super.onDismiss(dialog);
//        if (mSending != null) {
//            mSending.cancel();
//            mSending = null;
//        }
//        mParent.finish();
//    }
//
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        switch (requestCode) {
//            case REQUEST_SETUP:
//                mIsInSetup = false;
//                if (resultCode != Activity.RESULT_OK) {
//                    mParent.finish();
//                }
//                break;
//            default:
//                super.onActivityResult(requestCode, resultCode, data);
//        }
//    }
//
//    @Override
//    public void onPeerFound(Peer peer) {
//        Log.d(TAG, "Found: " + peer.id + " (" + peer.name + ")");
//        mPeers.put(peer.id, peer);
//        mAdapter.notifyDataSetChanged();
//    }
//
//    @Override
//    public void onPeerDisappeared(Peer peer) {
//        Log.d(TAG, "Disappeared: " + peer.id + " (" + peer.name + ")");
//        if (peer.id.equals(mPeerPicked)) {
//            mPeerPicked = null;
//        }
//        mPeers.remove(peer.id);
//        mAdapter.notifyDataSetChanged();
//    }
//
//    private boolean setupIfNeeded() {
//        if (mIsInSetup) {
//            return true;
//        }
//
//        final boolean granted = mParent.checkSelfPermission(WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED;
//        final boolean ready = mAirDropManager.ready() == STATUS_OK;
//        if (!granted || !ready) {
//            mIsInSetup = true;
//            startActivityForResult(new Intent(mParent, SetupActivity.class), REQUEST_SETUP);
//            return true;
//        } else {
//            return false;
//        }
//    }
//
//    private void handleItemClick(Peer peer) {
//        if (mPeerStatus != 0 && mPeerStatus != R.string.status_rejected) {
//            return;
//        }
//        mPeerStatus = 0;
//        if (peer.id.equals(mPeerPicked)) {
//            mPeerPicked = null;
//            mSendButton.setEnabled(false);
//        } else {
//            mPeerPicked = peer.id;
//            mSendButton.setEnabled(true);
//        }
//        mAdapter.notifyDataSetChanged();
//    }
//
//    private void handleSendConfirming() {
//        mPeerStatus = R.string.status_waiting_for_confirm;
//        mBytesTotal = -1;
//        mBytesSent = 0;
//        mAdapter.notifyDataSetChanged();
//        mSendButton.setEnabled(false);
//        mDiscoveringView.setVisibility(View.GONE);
//        mShouldKeepDiscovering = true;
//        mWakeLock.acquire();
//    }
//
//    private void handleSendRejected() {
//        mSending = null;
//        mPeerStatus = R.string.status_rejected;
//        mAdapter.notifyDataSetChanged();
//        mSendButton.setEnabled(true);
//        mDiscoveringView.setVisibility(View.VISIBLE);
//        mShouldKeepDiscovering = false;
//        mWakeLock.release();
//        Toast.makeText(getContext(), R.string.toast_rejected, Toast.LENGTH_SHORT).show();
//    }
//
//    private void handleSending() {
//        mPeerStatus = R.string.status_sending;
//        mAdapter.notifyDataSetChanged();
//    }
//
//    private void handleSendSucceed() {
//        mSending = null;
//        mShouldKeepDiscovering = false;
//        mWakeLock.release();
//        Toast.makeText(getContext(), R.string.toast_completed, Toast.LENGTH_SHORT).show();
//        mParent.setResult(Activity.RESULT_OK);
//        dismiss();
//    }
//
//    private void handleSendFailed() {
//        mSending = null;
//        mPeerPicked = null;
//        mPeerStatus = 0;
//        mAdapter.notifyDataSetChanged();
//        mSendButton.setEnabled(true);
//        mDiscoveringView.setVisibility(View.VISIBLE);
//        mShouldKeepDiscovering = false;
//        mWakeLock.release();
//    }
//
//    private <P extends Peer> void sendFile(P peer, List<Entity> entities) {
//        handleSendConfirming();
//
//        final SendListener listener = new SendListener() {
//            @Override
//            public void onAccepted() {
//                handleSending();
//            }
//
//            @Override
//            public void onRejected() {
//                handleSendRejected();
//            }
//
//            @Override
//            public void onProgress(long bytesSent, long bytesTotal) {
//                mBytesSent = bytesSent;
//                mBytesTotal = bytesTotal;
//                mAdapter.notifyDataSetChanged();
//            }
//
//            @Override
//            public void onSent() {
//                handleSendSucceed();
//            }
//
//            @Override
//            public void onSendFailed() {
//                handleSendFailed();
//            }
//        };
//
//        if (peer instanceof AirDropPeer) {
//            mSending = mAirDropManager.send((AirDropPeer) peer, entities, listener);
//        } else if (peer instanceof NearSharePeer) {
//            mSending = mNearShareManager.send((NearSharePeer) peer, entities, listener);
//        }
//    }
//


    public void onCancel() {
        mParent.finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mParent.finish();
    }
}