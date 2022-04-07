package org.exthmui.share.ui;

import static org.exthmui.share.shared.base.SendingWorker.P_BYTES_SENT;
import static org.exthmui.share.shared.base.SendingWorker.P_BYTES_TOTAL;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewTreeLifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.WorkInfo;

import org.exthmui.share.R;
import org.exthmui.share.shared.Constants;
import org.exthmui.share.shared.base.PeerInfo;

import java.util.Map;

public class PeersAdapter extends RecyclerView.Adapter<PeersAdapter.ViewHolder> {

    public static final int REQUEST_CODE_PICK_FILE = 0;

    private LayoutInflater mInflater;
    private final ArrayMap<String, PeerInfo> mPeers = new ArrayMap<>();

    @Nullable
    private OnPeerSelectedListener mOnPeerSelectedListener;

    private final MutableLiveData<String> mPeerSelectedLiveData = new MutableLiveData<>(null);

    public PeersAdapter() {
    }

    public void initialize(Context context) {
        mInflater = LayoutInflater.from(context);
    }

    public void setOnPeerSelectedListener(@Nullable OnPeerSelectedListener onPeerSelectedListener) {
        this.mOnPeerSelectedListener = onPeerSelectedListener;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
    }

    @NonNull
    @Override
    public PeersAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(mInflater.inflate(R.layout.item_peer, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull PeersAdapter.ViewHolder holder, int position) {
        final String id;
        id = mPeers.keyAt(position);
        final PeerInfo peer = mPeers.valueAt(position);
        holder.setPeer(peer);
        final boolean selected = id.equals(mPeerSelectedLiveData.getValue());
        holder.itemView.setSelected(selected);
        holder.itemView.setOnClickListener(view -> {
            if (mOnPeerSelectedListener == null || mOnPeerSelectedListener.onPeerSelected(peer, holder)) {
                mPeerSelectedLiveData.setValue(peer.getId());
            }
        });
    }

    @Override
    public long getItemId(int position) {
        return mPeers.keyAt(position).hashCode();
    }

    @Override
    public int getItemCount() {
        return mPeers.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setData(Map<String, PeerInfo> peers) {
        mPeers.clear();
        mPeers.putAll(peers);
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addPeer(PeerInfo peer) {
        mPeers.put(peer.getId(), peer);
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updatePeer(PeerInfo peer) {
        mPeers.replace(peer.getId(), peer);
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void removePeer(PeerInfo peer) {
        mPeers.remove(peer.getId());
        notifyDataSetChanged();
    }

    public MutableLiveData<String> getPeerSelectedLiveData() {
        return mPeerSelectedLiveData;
    }

    public String getPeerSelected() {
        return mPeerSelectedLiveData.getValue();
    }

    public void setPeerSelected(String peerSelected) {
        mPeerSelectedLiveData.setValue(peerSelected);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private PeerInfo peer;
        private final BadgeHelper badgeHelper;

        LifecycleOwner mLifecycleOwner;

        private final View mView;
        private final ConstraintLayout mPeerIconContainer;
        private final TextView mPeerNameText;
        private final TextView mPeerDetailText;
        private final ProgressBar mPeerProgressBar;
        private final ImageView mPeerIcon;

        ViewHolder(View view) {
            super(view);

            mView = view;
            mLifecycleOwner = ViewTreeLifecycleOwner.get(mView);
            mPeerIconContainer = mView.findViewById(R.id.peer_icon_container);
            mPeerNameText = mView.findViewById(R.id.peer_name_text);
            mPeerDetailText = mView.findViewById(R.id.peer_detail_text);
            mPeerProgressBar = mView.findViewById(R.id.peer_progress_bar);
            mPeerIcon = mView.findViewById(R.id.peer_icon);

            badgeHelper = new BadgeHelper(view.getContext())
                    .setBadgeOverlap(true)
                    .setBadgeType(BadgeHelper.Type.TYPE_POINT)
                    .setBadgeEnabled(true);
            badgeHelper.bindToTargetView(mPeerIcon);
        }

        public View getView() {
            return mView;
        }

        public ConstraintLayout getPeerIconContainer() {
            return mPeerIconContainer;
        }

        public TextView getPeerNameText() {
            return mPeerNameText;
        }

        public TextView getPeerDetailText() {
            return mPeerDetailText;
        }

        public ProgressBar getPeerProgressBar() {
            return mPeerProgressBar;
        }

        public ImageView getPeerIcon() {
            return mPeerIcon;
        }

        public PeerInfo getPeer() {
            return peer;
        }

        public void setPeer(@NonNull PeerInfo peer) {
            this.peer = peer;

            // Set transition name
            mPeerIconContainer.setTransitionName(mView.getContext().getString(R.string.transition_name_peer_icon_container, peer.getId()));
            mPeerProgressBar.setTransitionName(mView.getContext().getString(R.string.transition_name_peer_progress_bar, peer.getId()));
            mPeerIcon.setTransitionName(mView.getContext().getString(R.string.transition_name_peer_icon, peer.getId()));
            mPeerNameText.setTransitionName(mView.getContext().getString(R.string.transition_name_peer_name_text, peer.getId()));
            mPeerDetailText.setTransitionName(mView.getContext().getString(R.string.transition_name_peer_detail_text, peer.getId()));

            mPeerNameText.setText(peer.getDisplayName());
            Constants.DeviceType deviceType = Constants.DeviceType.parse(peer.getDeviceType());
            if (deviceType != null) mPeerIcon.setImageResource(deviceType.getImgRes());

            int cStatus = peer.getConnectionStatus();
            int tStatus = peer.getTransmissionStatus();
            if (tStatus == Constants.TransmissionStatus.IN_PROGRESS.getNumVal()) {
                mPeerDetailText.setVisibility(View.VISIBLE);
                peer.getAllWorkInfosLiveData(mView.getContext())
                        .observe(mLifecycleOwner, workInfos -> {
                            Context context = itemView.getContext();
                            String detailText = null;
                            long bytesSent = -1;
                            long bytesTotal = -1;

                            // add badge
                            int succeededNumber = 0;
                            int failedNumber = 0;
                            int waitingNumber = 0;
                            for (WorkInfo workInfo : workInfos) {
                                if (workInfo == null) continue;
                                switch (workInfo.getState()) {
                                    case RUNNING:
                                        Data progress_data = workInfo.getProgress();
                                        bytesSent += progress_data.getLong(P_BYTES_SENT, 0);
                                        bytesTotal += progress_data.getLong(P_BYTES_TOTAL, 0);
                                        if (bytesSent == -1 | bytesTotal == -1) {
                                            bytesSent++;
                                            bytesTotal++;
                                        }
                                        detailText = context.getString(R.string.status_sending_progress,
                                                Formatter.formatFileSize(context, bytesSent),
                                                Formatter.formatFileSize(context, bytesTotal));
                                        break;
                                    case FAILED:
                                        failedNumber++;
                                        continue;
                                    case BLOCKED:
                                    case ENQUEUED:
                                        waitingNumber++;
                                        continue;
                                    case SUCCEEDED:
                                        succeededNumber++;
                                }
                            }
                            if (failedNumber != 0)
                                addBadge(mView.getContext().getResources().getColor(R.color.transmission_failed_badge_background, null), failedNumber);
                            else if (waitingNumber != 0)
                                addBadge(mView.getContext().getResources().getColor(R.color.transmission_waiting_badge_background, null), waitingNumber);
                            else if (succeededNumber != 0)
                                addBadge(mView.getContext().getResources().getColor(R.color.transmission_succeeded_badge_background, null), succeededNumber);
                            mPeerDetailText.setText(detailText);
                            if (bytesSent == -1 | bytesTotal == -1) {
                                mPeerProgressBar.setIndeterminate(true);
                            } else {
                                mPeerProgressBar.setIndeterminate(false);
                                mPeerProgressBar.setMax((int) bytesTotal);
                                mPeerProgressBar.setProgress((int) bytesSent, true);
                            }
                        });
            } else if (peer.getDetailMessage() == null)
                mPeerDetailText.setVisibility(View.GONE);
            else if (peer.getDetailMessage().isEmpty())
                mPeerDetailText.setVisibility(View.GONE);
            else {
                mPeerDetailText.setVisibility(View.VISIBLE);
                mPeerDetailText.setText(peer.getDetailMessage());
            }
            if (cStatus == Constants.ConnectionStatus.TRANSMITTING.getNumVal() &&
                    tStatus != Constants.TransmissionStatus.IN_PROGRESS.getNumVal()) {
                mPeerProgressBar.setVisibility(View.VISIBLE);
                mPeerProgressBar.setIndeterminate(true);
            } else {
                mPeerProgressBar.setVisibility(View.GONE);
            }
        }

        void addBadge(@ColorInt int color) {
            badgeHelper
                    .setBadgeColor(color)
                    .setBadgeType(BadgeHelper.Type.TYPE_POINT)
                    .setBadgeOverlap(true)
                    .setIgnoreTargetPadding(true)
                    .setBadgeEnabled(true);
        }

        void addBadge(@ColorInt int color, int number) {
            badgeHelper
                    .setBadgeColor(color)
                    .setBadgeType(BadgeHelper.Type.TYPE_TEXT)
                    .setBadgeOverlap(true)
                    .setIgnoreTargetPadding(true)
                    .setBadgeText(String.valueOf(number))
                    .setBadgeEnabled(true);
        }

        void removeBadge() {
            badgeHelper.setBadgeEnabled(false);
        }
    }

    public interface OnPeerSelectedListener {
        /**
         * On peer selected
         *
         * @param peer The peer requested to be selected
         * @return Whether to select the peer
         */
        boolean onPeerSelected(PeerInfo peer, ViewHolder viewHolder);
    }
}