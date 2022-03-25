package org.exthmui.share.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.exthmui.share.PeersAdapter;
import org.exthmui.share.R;
import org.exthmui.share.shared.CrossFadeUtils;
import org.exthmui.share.shared.base.PeerInfo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

public class PeerChooserView extends FrameLayout {

    public static final String TAG = "PeerChooserView";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STATE_ENABLED_NO_PEER, STATE_DISABLED, STATE_UNAVAILABLE})
    public @interface PublicState{}
    @IntDef({STATE_NOT_INITIALIZED, STATE_ENABLED, STATE_ENABLED_NO_PEER, STATE_DISABLED, STATE_UNAVAILABLE})
    public @interface State{}
    private static final int STATE_NOT_INITIALIZED = -1;
    private static final int STATE_ENABLED = 0;
    public static final int STATE_ENABLED_NO_PEER = 1;
    public static final int STATE_DISABLED = 2;
    public static final int STATE_UNAVAILABLE = 3;

    @State
    private int mState = STATE_NOT_INITIALIZED;

    private ViewGroup mScanningPlaceholder;
    private ViewGroup mNonScanningPlaceholder;
    private ViewGroup mUnavailablePlaceholder;
    private WaveView mWaveView;
    private Button mEnableButton;
    private RecyclerView mRecyclerView;
    private PeersAdapter mAdapter;

    public PeerChooserView(Context context) {
        super(context);
        initView(context);
    }

    public PeerChooserView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public PeerChooserView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    public PeerChooserView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }

    private void initView(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_peer_chooser, this, true);

        mScanningPlaceholder = view.findViewById(R.id.view_peer_chooser_scanning_placeholder);
        mWaveView = mScanningPlaceholder.findViewById(R.id.scanning_placeholder_wave_view);
        mNonScanningPlaceholder = view.findViewById(R.id.view_peer_chooser_non_scanning_placeholder);
        mUnavailablePlaceholder = view.findViewById(R.id.view_peer_chooser_unavailable_placeholder);
        mRecyclerView = view.findViewById(R.id.view_peer_chooser_recycler_view);

        mWaveView.setWaveColor(getContext().getColor(R.color.placeholder_wave_color));
        mWaveView.startWave();

        mEnableButton = mNonScanningPlaceholder.findViewById(R.id.non_scanning_placeholder_enable_button);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        mRecyclerView.setLayoutManager(layoutManager);

        mAdapter = new PeersAdapter(getContext());
        mRecyclerView.setAdapter(mAdapter);
    }

    public void setData(Map<String, PeerInfo> peers) {
        mAdapter.setData(peers);
        if (peers.isEmpty()) {
            setState(STATE_ENABLED_NO_PEER);
        } else setStateInternal(STATE_ENABLED);
    }

    public void addPeer(PeerInfo peer) {
        mAdapter.addPeer(peer);
        setStateInternal(STATE_ENABLED);
    }

    public void updatePeer(PeerInfo peer) {
        mAdapter.updatePeer(peer);
    }

    public void removePeer(PeerInfo peer) {
        mAdapter.removePeer(peer);
        if (mAdapter.getItemCount() == 0) {
            setState(STATE_ENABLED_NO_PEER);
        }
    }

    public String getPeerSelected() {
        return mAdapter.getPeerSelected();
    }

    public MutableLiveData<String> getPeerSelectedLiveData() {
        return mAdapter.getPeerSelectedLiveData();
    }


    public void discoverStarted() {
        if (mState != STATE_ENABLED & mState != STATE_ENABLED_NO_PEER) {
            setState(STATE_ENABLED_NO_PEER);
        }
    }

    /**
     * Set state of scanning service
     * ATTENTION: Can be overwritten by {@link #setData(Map)}, {@link #addPeer(PeerInfo)},
     * {@link #removePeer(PeerInfo)}
     * @param state A value in range of {@link #STATE_ENABLED_NO_PEER}, {@link #STATE_DISABLED},
     * {@link #STATE_UNAVAILABLE}
     */
    public void setState(@PublicState int state) {
        setStateInternal(state);
    }

    private void setStateInternal(@State int state) {
        if (mState == state) return;
        long duration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);
        switch (state) {
            case STATE_NOT_INITIALIZED:
                break;
            case STATE_ENABLED:
                mState = state;
                CrossFadeUtils.fadeOut(mScanningPlaceholder, duration, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mWaveView.stopWave();
                    }
                });
                CrossFadeUtils.fadeOut(mNonScanningPlaceholder, duration);
                CrossFadeUtils.fadeOut(mUnavailablePlaceholder, duration);
                CrossFadeUtils.fadeIn(mRecyclerView, duration);
                break;
            case STATE_ENABLED_NO_PEER:
                mState = state;
                CrossFadeUtils.fadeOut(mRecyclerView, duration);
                CrossFadeUtils.fadeOut(mNonScanningPlaceholder, duration);
                CrossFadeUtils.fadeOut(mUnavailablePlaceholder, duration);

                CrossFadeUtils.fadeIn(mScanningPlaceholder, duration, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mWaveView.startWave();
                    }
                });
                break;
            case STATE_DISABLED:
                mState = state;
                CrossFadeUtils.fadeOut(mRecyclerView, duration);
                CrossFadeUtils.fadeOut(mScanningPlaceholder, duration, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mWaveView.stopWave();
                    }
                });
                CrossFadeUtils.fadeOut(mUnavailablePlaceholder, duration);

                CrossFadeUtils.fadeIn(mNonScanningPlaceholder, duration);
                break;
            case STATE_UNAVAILABLE:
                mState = state;
                CrossFadeUtils.fadeOut(mRecyclerView, duration);
                CrossFadeUtils.fadeOut(mScanningPlaceholder, duration, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mWaveView.stopWave();
                    }
                });
                CrossFadeUtils.fadeOut(mNonScanningPlaceholder, duration);

                CrossFadeUtils.fadeIn(mUnavailablePlaceholder, duration);
                break;
            default:
                Log.e(TAG, String.format("Unknown state: %d", state));
        }
    }

    public void setEnableButtonOnClickListener(OnClickListener l) {
        mEnableButton.setOnClickListener(l);
    }
}
