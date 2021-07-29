package org.exthmui.share;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import org.exthmui.share.beans.Peer;
import org.exthmui.share.beans.PeerInfo;
import org.exthmui.share.misc.Constants;

public class PeersAdapter extends RecyclerView.Adapter<PeersAdapter.ViewHolder> {

    private final LayoutInflater mInflater;
    private ArrayMap<String, PeerInfo> mPeers;

    private String mPeerSelected;

    PeersAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public PeersAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PeersAdapter.ViewHolder(parent.getContext(), mInflater.inflate(R.layout.item_peer, parent, false));
    }
//TODO:Fix it
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onBindViewHolder(@NonNull PeersAdapter.ViewHolder holder, int position) {
        final String id;
        id = mPeers.keyAt(position);
        final PeerInfo peer = mPeers.valueAt(position);
        final boolean selected = id.equals(mPeerSelected);
        holder.nameView.setText(peer.getDisplayName());
        holder.itemView.setSelected(selected);
        int tStatus = peer.getTransmissionStatus();
        if (tStatus == Constants.TransmissionStatus.TRANSMITTNG.getNumVal()){
            holder.detailView.setVisibility(View.VISIBLE);
            holder.detailView.setText(holder.context.getString(R.string.status_sending_progress,
                    Formatter.formatFileSize(holder.context, bytesSent),
                    Formatter.formatFileSize(holder.context, bytesTotal)));
        }
        if (!peer.getDetailMessage().isEmpty()) {
            holder.detailView.setVisibility(View.VISIBLE);
            if (pee == R.string.status_sending && mBytesTotal != -1) {
            } else {
                holder.detailView.setText(mPeerStatus);
            }
        } else {
            holder.detailView.setVisibility(View.GONE);
        }
        if (selected && mPeerStatus != 0 && mPeerStatus != R.string.status_rejected) {
            holder.progressBar.setVisibility(View.VISIBLE);
            if (mBytesTotal == -1 || mPeerStatus != R.string.status_sending) {
                holder.progressBar.setIndeterminate(true);
            } else {
                holder.progressBar.setIndeterminate(false);
                holder.progressBar.setMax((int) mBytesTotal);
                holder.progressBar.setProgress((int) mBytesSent, true);
            }
        } else {
            holder.progressBar.setVisibility(View.GONE);
        }
        if (peer instanceof AirDropPeer) {
            final boolean isMokee = ((AirDropPeer) peer).getMokeeApiVersion() > 0;
            if (isMokee) {
                holder.iconView.setImageResource(R.drawable.ic_mokee_24dp);
            } else {
                holder.iconView.setImageResource(R.drawable.ic_apple_24dp);
            }
        } else if (peer instanceof NearSharePeer) {
            holder.iconView.setImageResource(R.drawable.ic_windows_24dp);
        } else {
            holder.iconView.setImageDrawable(null);
        }
        holder.itemView.setOnClickListener(v -> handleItemClick(peer));
    }

    @Override
    public long getItemId(int position) {
        return mPeers.keyAt(position).hashCode();
    }

    @Override
    public int getItemCount() {
        return mPeers.size();
    }

    public void setData(ArrayMap<String, Peer> peers) {
        mPeers = peers;
    }


    class ViewHolder extends RecyclerView.ViewHolder {

        private Context context;
        ImageView iconView;
        TextView nameView;
        TextView detailView;

        ViewHolder(@NonNull Context context, @NonNull View itemView) {
            super(itemView);
            this.context = context;
            iconView = itemView.findViewById(R.id.iconView);
            nameView = itemView.findViewById(R.id.text_name);
            detailView = itemView.findViewById(R.id.text_detail);
        }

    }

}