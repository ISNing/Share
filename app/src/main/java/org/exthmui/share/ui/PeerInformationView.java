package org.exthmui.share.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import org.exthmui.share.R;
import org.exthmui.share.shared.base.IPeer;
import org.exthmui.share.shared.misc.Constants;
import org.exthmui.share.shared.misc.IConnectionType;

public class PeerInformationView extends ConstraintLayout {

    public static final String TAG = "PeerInformationView";

    @Nullable
    private IPeer mPeer;
    private OnBackPressedListener mOnBackPressedListener;

    private TextView mPeerNameText;
    private TextView mPeerDeviceTypeText;
    private TextView mPeerPluginText;
    private TextView mPeerDetailText;
    private ImageView mPeerIcon;

    public PeerInformationView(@NonNull Context context) {
        super(context);
        initView(context);
    }

    public PeerInformationView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public PeerInformationView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    public PeerInformationView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }

    private void initView(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_peer_information, this, true);
        ImageButton backButton = view.findViewById(R.id.view_peer_information_back_button);
        backButton.setOnClickListener(view1 -> {
            if (mOnBackPressedListener != null)
                mOnBackPressedListener.onBackPressed(this, mPeer);
        });
        mPeerNameText = view.findViewById(R.id.peer_name_text);
        mPeerDeviceTypeText = view.findViewById(R.id.peer_device_type_text);
        mPeerPluginText = view.findViewById(R.id.peer_plugin_text);
        mPeerDetailText = view.findViewById(R.id.peer_detail_text);
        mPeerIcon = view.findViewById(R.id.peer_icon);
    }

    public void setPeer(@Nullable IPeer peer) {
        mPeer = peer;
        if (peer == null) {
            mPeerIcon.setImageResource(Constants.DeviceType.UNKNOWN.getImgRes());
            mPeerNameText.setText("");
            mPeerPluginText.setText("");
            mPeerDeviceTypeText.setText("");
            mPeerDetailText.setText("");
            mPeerNameText.setTransitionName("");
            mPeerDeviceTypeText.setTransitionName("");
            mPeerPluginText.setTransitionName("");
            mPeerDetailText.setTransitionName("");
            mPeerIcon.setTransitionName("");
            return;
        }

        // Set transition name
        mPeerNameText.setTransitionName(getContext().getString(R.string.transition_name_peer_name_text, mPeer.getId()));
        mPeerDeviceTypeText.setTransitionName(getContext().getString(R.string.transition_name_peer_device_type_text, mPeer.getId()));
        mPeerPluginText.setTransitionName(getContext().getString(R.string.transition_name_peer_plugin_text, mPeer.getId()));
        mPeerDetailText.setTransitionName(getContext().getString(R.string.transition_name_peer_detail_text, mPeer.getId()));
        mPeerIcon.setTransitionName(getContext().getString(R.string.transition_name_peer_icon, mPeer.getId()));

        mPeerNameText.setText(peer.getDisplayName());
        Constants.DeviceType deviceType = Constants.DeviceType.parse(peer.getDeviceType());
        IConnectionType connectionType = peer.getConnectionType();
        if (deviceType != null) {
            mPeerIcon.setImageResource(deviceType.getImgRes());
            mPeerDeviceTypeText.setText(deviceType.getFriendlyNameRes());
        } else Log.e(TAG, "Got null deviceType!");
        mPeerPluginText.setText(connectionType.getFriendlyName());

        if (peer.getDetailMessage() == null || peer.getDetailMessage().isEmpty())
            mPeerDetailText.setText("");
        else {
            mPeerDetailText.setText(peer.getDetailMessage());
        }
    }

    @Nullable
    public IPeer getPeer() {
        return mPeer;
    }

    public OnBackPressedListener getOnBackPressedListener() {
        return mOnBackPressedListener;
    }

    public TextView getPeerNameText() {
        return mPeerNameText;
    }

    public TextView getPeerDeviceTypeText() {
        return mPeerDeviceTypeText;
    }

    public TextView getPeerPluginText() {
        return mPeerPluginText;
    }

    public TextView getPeerDetailText() {
        return mPeerDetailText;
    }

    public ImageView getPeerIcon() {
        return mPeerIcon;
    }

    public void setOnBackPressedListener(OnBackPressedListener l) {
        mOnBackPressedListener = l;
    }

    public interface OnBackPressedListener {
        /**
         * On back pressed
         */
        void onBackPressed(PeerInformationView view, IPeer peer);
    }
}
