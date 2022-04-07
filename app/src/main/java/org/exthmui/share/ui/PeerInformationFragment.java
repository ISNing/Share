package org.exthmui.share.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.google.android.material.transition.MaterialContainerTransform;
import com.google.android.material.transition.MaterialFadeThrough;

import org.exthmui.share.R;
import org.exthmui.share.shared.base.PeerInfo;

public class PeerInformationFragment extends Fragment {

    public static final String TAG = "PeerInformationFragment";

    private PeerInformationView mView;
    private PeerInfo mPeer;
    private PeerInformationView.OnBackPressedListener mOnBackPressedListener;

    public PeerInformationFragment() {
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragment = inflater.inflate(R.layout.fragment_peer_information, container, false);
        mView = fragment.findViewById(R.id.fragment_peer_information_view);
        if (mPeer != null) mView.setPeer(mPeer);
        if (mOnBackPressedListener != null) {
            mView.setOnBackPressedListener(mOnBackPressedListener);
            mOnBackPressedListener = null;
        }
        return fragment;
    }

    public void setPeer(PeerInfo peer) {
        mPeer = peer;
        if (mView != null) mView.setPeer(mPeer);
    }

    public PeerInfo getPeer() {
        return mView == null ? mPeer : mView.getPeer();
    }

    public PeerInformationView getPeerInfoView() {
        return mView;
    }

    public void setOnBackPressedListener(PeerInformationView.OnBackPressedListener l) {
        if (mView == null) mOnBackPressedListener = l;
        else mView.setOnBackPressedListener(l);
    }
}