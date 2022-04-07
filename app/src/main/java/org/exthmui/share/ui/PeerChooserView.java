package org.exthmui.share.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.MutableLiveData;

import org.exthmui.share.R;
import org.exthmui.share.shared.base.PeerInfo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

public class PeerChooserView extends FrameLayout {

    public static final String TAG = "PeerChooserView";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STATE_ENABLED_NO_PEER, STATE_DISABLED, STATE_UNAVAILABLE})
    public @interface PublicState {
    }

    @IntDef({STATE_NOT_INITIALIZED, STATE_ENABLED, STATE_ENABLED_NO_PEER, STATE_DISABLED, STATE_UNAVAILABLE})
    public @interface State {
    }

    public static final int STATE_NOT_INITIALIZED = -1;
    public static final int STATE_ENABLED = 0;
    public static final int STATE_ENABLED_NO_PEER = 1;
    public static final int STATE_DISABLED = 2;
    public static final int STATE_UNAVAILABLE = 3;

    @State
    private int mState = STATE_NOT_INITIALIZED;

    private final PeerChooserFragment mPeerChooserFragment = new PeerChooserFragment();
    private final PeerInformationFragment mPeerInformationFragment = new PeerInformationFragment();

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
        LayoutInflater.from(context).inflate(R.layout.view_peer_chooser, this, true);
    }

    public void initialize(@NonNull FragmentManager fragmentManager) {
        fragmentManager.beginTransaction()
                .add(R.id.view_peer_chooser_fragment_container, mPeerChooserFragment, PeerChooserFragment.TAG)
                .add(R.id.view_peer_chooser_fragment_container, mPeerInformationFragment, PeerInformationFragment.TAG)
                .hide(mPeerInformationFragment)
                .show(mPeerChooserFragment)
                .addToBackStack(PeerChooserFragment.TAG)
                .commit();
        mPeerChooserFragment.setOnPeerSelectedListener(new PeersAdapter.OnPeerSelectedListener() {
            FragmentTransaction addSharedElements(FragmentTransaction transaction, PeerInfo peer, PeersAdapter.ViewHolder holder) {
                return transaction.addSharedElement(holder.getPeerIcon(),
                        getContext().getString(R.string.transition_name_peer_icon, peer.getId()));
            }

            @Override
            public boolean onPeerSelected(PeerInfo peer, PeersAdapter.ViewHolder holder) {
                mPeerInformationFragment.setPeer(peer);
                addSharedElements(fragmentManager.beginTransaction(), peer, holder)
                        .hide(mPeerChooserFragment)
                        .show(mPeerInformationFragment)
                        .commit();
                return true;
            }
        });
        mPeerInformationFragment.setOnBackPressedListener(new PeerInformationView.OnBackPressedListener() {
            FragmentTransaction addSharedElements(FragmentTransaction transaction, PeerInfo peer, PeerInformationView view) {
                return transaction.addSharedElement(view.getPeerIcon(),
                        getContext().getString(R.string.transition_name_peer_icon, peer.getId()));
            }

            @Override
            public void onBackPressed(PeerInformationView view, PeerInfo peer) {
                mPeerChooserFragment.getPeerSelectedLiveData().setValue(null);
                addSharedElements(fragmentManager.beginTransaction(), peer, view)
                        .hide(mPeerInformationFragment)
                        .show(mPeerChooserFragment)
                        .commit();
            }
        });
    }

    public void setData(Map<String, PeerInfo> peers) {
        mPeerChooserFragment.setData(peers);
        if (peers.isEmpty()) {
            setState(STATE_ENABLED_NO_PEER);
        } else setStateInternal(STATE_ENABLED);
    }

    public void addPeer(PeerInfo peer) {
        mPeerChooserFragment.addPeer(peer);
        setStateInternal(STATE_ENABLED);
    }

    public void updatePeer(PeerInfo peer) {
        mPeerChooserFragment.updatePeer(peer);
    }

    public void removePeer(PeerInfo peer) {
        mPeerChooserFragment.removePeer(peer);
        if (mPeerChooserFragment.getAdapter().getItemCount() == 0) {
            setState(STATE_ENABLED_NO_PEER);
        }
    }

    public String getPeerSelected() {
        return mPeerChooserFragment.getPeerSelected();
    }

    public MutableLiveData<String> getPeerSelectedLiveData() {
        return mPeerChooserFragment.getPeerSelectedLiveData();
    }


    public void discoverStarted() {
        if (mState != STATE_ENABLED & mState != STATE_ENABLED_NO_PEER) {
            setState(STATE_ENABLED_NO_PEER);
        }
    }

    public int getState() {
        return mPeerChooserFragment.getState();
    }

    /**
     * Set state of scanning service
     * ATTENTION: Can be overwritten by {@link #setData(Map)}, {@link #addPeer(PeerInfo)},
     * {@link #removePeer(PeerInfo)}
     *
     * @param state A value in range of {@link #STATE_ENABLED_NO_PEER}, {@link #STATE_DISABLED},
     *              {@link #STATE_UNAVAILABLE}
     */
    public void setState(@PublicState int state) {
        setStateInternal(state);
    }

    private void setStateInternal(@State int state) {
        if (mState == state) return;
        switch (state) {
            case STATE_NOT_INITIALIZED:
                break;
            case STATE_ENABLED:
            case STATE_ENABLED_NO_PEER:
            case STATE_DISABLED:
            case STATE_UNAVAILABLE:
                mState = state;
                mPeerChooserFragment.setState(mState);
                break;
            default:
                Log.e(TAG, String.format("Unknown state: %d", state));
        }
    }

    public void setEnableButtonOnClickListener(OnClickListener l) {
        mPeerChooserFragment.setEnableButtonOnClickListener(l);
    }
}
