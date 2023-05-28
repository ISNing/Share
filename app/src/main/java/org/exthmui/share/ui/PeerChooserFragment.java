package org.exthmui.share.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.transition.MaterialContainerTransform;
import com.google.android.material.transition.MaterialFadeThrough;

import org.exthmui.share.R;
import org.exthmui.share.shared.base.IPeer;
import org.exthmui.utils.CrossFadeUtils;

import java.util.Map;

public class PeerChooserFragment extends Fragment {

    public static final String TAG = "PeerChooserFragment";

    private ViewGroup mScanningPlaceholder;
    private ViewGroup mNonScanningPlaceholder;
    private ViewGroup mUnavailablePlaceholder;
    private WaveView mWaveView;
    private Button mEnableButton;
    private RecyclerView mRecyclerView;
    private final PeersAdapter mAdapter = new PeersAdapter();

    @Nullable
    private View.OnClickListener mEnableButtonOnClickListener;

    @PeerChooserView.State
    private int mState = PeerChooserView.STATE_NOT_INITIALIZED;

    private boolean stateFlag = false;

    public PeerChooserFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MaterialContainerTransform transform = new MaterialContainerTransform();
        transform.setScrimColor(Color.TRANSPARENT);
        setSharedElementEnterTransition(transform);
        setEnterTransition(new MaterialFadeThrough());
        setReenterTransition(new MaterialFadeThrough());
        setExitTransition(new MaterialFadeThrough());
        mAdapter.initialize(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_peer_chooser, container, false);

        mScanningPlaceholder = view.findViewById(R.id.fragment_peer_chooser_scanning_placeholder);
        mWaveView = mScanningPlaceholder.findViewById(R.id.scanning_placeholder_wave_view);
        mNonScanningPlaceholder = view.findViewById(R.id.fragment_peer_chooser_non_scanning_placeholder);
        mUnavailablePlaceholder = view.findViewById(R.id.fragment_peer_chooser_unavailable_placeholder);
        mRecyclerView = view.findViewById(R.id.fragment_peer_chooser_recycler_view);

        mWaveView.setWaveColor(requireContext().getColor(R.color.placeholder_wave_color));

        mEnableButton = mNonScanningPlaceholder.findViewById(R.id.non_scanning_placeholder_enable_button);
        if (mEnableButtonOnClickListener != null) {
            mEnableButton.setOnClickListener(mEnableButtonOnClickListener);
            mEnableButtonOnClickListener = null;
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        mRecyclerView.setLayoutManager(layoutManager);

        mRecyclerView.setAdapter(mAdapter);
        return view;
    }

    @PeerChooserView.State
    int getState() {
        return mState;
    }

    void setState(@PeerChooserView.State int state) {
        if (mState == state && !stateFlag) return;
        stateFlag = false;
        if (!isAdded()) {
            stateFlag = true;
            return;
        }
        long duration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);
        switch (state) {
            case PeerChooserView.STATE_NOT_INITIALIZED:
                break;
            case PeerChooserView.STATE_ENABLED:
                mState = state;
                CrossFadeUtils.fadeOut(mScanningPlaceholder, duration, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mWaveView.stopWave();
                    }
                }).start();
                CrossFadeUtils.fadeOut(mNonScanningPlaceholder, duration).start();
                CrossFadeUtils.fadeOut(mUnavailablePlaceholder, duration).start();

                CrossFadeUtils.fadeIn(mRecyclerView, duration).start();
                break;
            case PeerChooserView.STATE_ENABLED_NO_PEER:
                mState = state;
                CrossFadeUtils.fadeOut(mRecyclerView, duration).start();
                CrossFadeUtils.fadeOut(mNonScanningPlaceholder, duration).start();
                CrossFadeUtils.fadeOut(mUnavailablePlaceholder, duration).start();

                CrossFadeUtils.fadeIn(mScanningPlaceholder, duration, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mWaveView.startWave();
                    }
                }).start();
                break;
            case PeerChooserView.STATE_DISABLED:
                mState = state;
                CrossFadeUtils.fadeOut(mRecyclerView, duration);
                CrossFadeUtils.fadeOut(mScanningPlaceholder, duration, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mWaveView.stopWave();
                    }
                }).start();
                CrossFadeUtils.fadeOut(mUnavailablePlaceholder, duration).start();

                CrossFadeUtils.fadeIn(mNonScanningPlaceholder, duration).start();
                break;
            case PeerChooserView.STATE_UNAVAILABLE:
                mState = state;
                CrossFadeUtils.fadeOut(mRecyclerView, duration).start();
                CrossFadeUtils.fadeOut(mScanningPlaceholder, duration, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mWaveView.stopWave();
                    }
                }).start();
                CrossFadeUtils.fadeOut(mNonScanningPlaceholder, duration).start();

                CrossFadeUtils.fadeIn(mUnavailablePlaceholder, duration).start();
                break;
            default:
                Log.e(TAG, String.format("Unknown state: %d", state));
        }
    }

    void setData(Map<String, IPeer> peers) {
        mAdapter.setData(peers);
    }

    void addPeer(@NonNull IPeer peer) {
        mAdapter.addPeer(peer);
    }

    void updatePeer(@NonNull IPeer peer) {
        mAdapter.updatePeer(peer);
    }

    void removePeer(@NonNull IPeer peer) {
        mAdapter.removePeer(peer);
    }

    @Nullable
    String getPeerSelected() {
        return mAdapter.getPeerSelected();
    }

    @Nullable
    MutableLiveData<String> getPeerSelectedLiveData() {
        return mAdapter.getPeerSelectedLiveData();
    }

    @NonNull
    PeersAdapter getAdapter() {
        return mAdapter;
    }

    void setEnableButtonOnClickListener(View.OnClickListener l) {
        if (mEnableButton == null) mEnableButtonOnClickListener = l;
        else mEnableButton.setOnClickListener(l);
    }

    void setOnPeerSelectedListener(@Nullable PeersAdapter.OnPeerSelectedListener l) {
        mAdapter.setOnPeerSelectedListener(l);
    }
}