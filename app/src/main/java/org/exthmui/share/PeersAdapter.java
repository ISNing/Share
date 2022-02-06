package org.exthmui.share;

import static org.exthmui.share.shared.base.SendingWorker.P_BYTES_SENT;
import static org.exthmui.share.shared.base.SendingWorker.P_BYTES_TOTAL;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewTreeLifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.WorkInfo;

import org.exthmui.share.databinding.ItemPeerBinding;
import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.base.PeerInfo;
import org.exthmui.share.shared.Constants;
import org.exthmui.share.ui.BadgeHelper;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PeersAdapter extends RecyclerView.Adapter<PeersAdapter.ViewHolder> {

    public void setEntities(List<Entity> entities) {
        if(mEntities == null) mEntities = entities;
    }

    public static final int REQUEST_CODE_PICK_FILE = 0;

    private List<Entity> mEntities;

    private final LayoutInflater mInflater;
    private ArrayMap<String, PeerInfo> mPeers = new ArrayMap<>();

    private final Context mContext;
    private LifecycleOwner mLifecycleOwner;

    private MutableLiveData<String> mPeerSelectedLiveData = new MutableLiveData<>(null);

    PeersAdapter(Context context) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
    }

    @NonNull
    @Override
    public PeersAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.mLifecycleOwner = ViewTreeLifecycleOwner.get(parent);
        ItemPeerBinding binding = ItemPeerBinding.inflate(mInflater, parent, false);
        ViewHolder viewHolder = new PeersAdapter.ViewHolder(binding.getRoot());
        viewHolder.bind(binding);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull PeersAdapter.ViewHolder holder, int position) {
        final String id;
        id = mPeers.keyAt(position);
        final PeerInfo peer = mPeers.valueAt(position);
        holder.peer = peer;
        final boolean selected = id.equals(mPeerSelectedLiveData.getValue());
        holder.binding.peerNameText.setText(peer.getDisplayName());
        holder.itemView.setSelected(selected);
        Constants.DeviceType deviceType = Constants.DeviceType.parse(peer.getDeviceType());
        if(deviceType != null) holder.binding.peerIcon.setImageResource(deviceType.getImgRes());

        int cStatus = peer.getConnectionStatus();
        int tStatus = peer.getTransmissionStatus();
        if (tStatus == Constants.TransmissionStatus.IN_PROGRESS.getNumVal()) {
            holder.binding.peerDetailText.setVisibility(View.VISIBLE);
            peer.getAllWorkInfosLiveData(mContext)
                    .observe(mLifecycleOwner, workInfos -> {
                        Context context = holder.binding.getRoot().getContext();
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
                                    if(bytesSent == -1 | bytesTotal == -1) {
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
                        if(failedNumber != 0) holder.addBadge(mContext.getResources().getColor(R.color.transmission_failed_badge_background), failedNumber);
                        else if(waitingNumber != 0) holder.addBadge(mContext.getResources().getColor(R.color.transmission_waiting_badge_background), waitingNumber);
                        else if(succeededNumber != 0) holder.addBadge(mContext.getResources().getColor(R.color.transmission_succeeded_badge_background), succeededNumber);
                        holder.binding.peerDetailText.setText(detailText);
                        if(bytesSent == -1 | bytesTotal == -1) {
                            holder.binding.peerProgressBar.setIndeterminate(true);
                        } else {
                            holder.binding.peerProgressBar.setIndeterminate(false);
                            holder.binding.peerProgressBar.setMax((int) bytesTotal);
                            holder.binding.peerProgressBar.setProgress((int) bytesSent, true);
                        }
                    });
        } else if (peer.getDetailMessage() == null)
            holder.binding.peerDetailText.setVisibility(View.GONE);
        else if (peer.getDetailMessage().isEmpty())
            holder.binding.peerDetailText.setVisibility(View.GONE);
        else {
            holder.binding.peerDetailText.setVisibility(View.VISIBLE);
            holder.binding.peerDetailText.setText(peer.getDetailMessage());
        }
        if (cStatus == Constants.ConnectionStatus.TRANSMITTING.getNumVal() &&
        tStatus != Constants.TransmissionStatus.IN_PROGRESS.getNumVal()) {
            holder.binding.peerProgressBar.setVisibility(View.VISIBLE);
            holder.binding.peerProgressBar.setIndeterminate(true);
        } else {
            holder.binding.peerProgressBar.setVisibility(View.GONE);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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

        ItemPeerBinding binding;
        private PeerInfo peer;
        private BadgeHelper badgeHelper;
        private boolean selected;

        ViewHolder(View view) {
            super(view);

        }

        void bind(ItemPeerBinding binding) {
            this.binding = binding;
            badgeHelper = new BadgeHelper(binding.getRoot().getContext()).setBadgeEnable(false);
            badgeHelper.bindToTargetView(binding.peerIconContainer);
        }

        void setSelected(boolean selected) {
            this.selected = selected;
            // TODO: add selected icon
        }

        void addBadge(@ColorInt int color){
            badgeHelper
                    .setBadgeColor(color)
                    .setBadgeType(BadgeHelper.Type.TYPE_POINT)
                    .setBadgeOverlap(true, true)
                    .setBadgeEnable(true);
        }
        void addBadge(@ColorInt int color, int number){
            badgeHelper
                    .setBadgeColor(color)
                    .setBadgeType(BadgeHelper.Type.TYPE_TEXT)
                    .setBadgeOverlap(true, true)
                    .setBadgeNumber(number)
                    .setBadgeEnable(true);
        }
        void removeBadge(){
            badgeHelper.setBadgeEnable(false);
        }
    }

}