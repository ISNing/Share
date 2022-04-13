package org.exthmui.share.shared.base;

import androidx.annotation.NonNull;

import java.io.Serializable;

public class PeerInfoTransfer implements Serializable {
    private String displayName;
    private String id;
    /**
     * Optional
     */
    private String protocolVersion;
    private int uid;
    private String accountServerSign;

    public PeerInfoTransfer() {

    }

    public PeerInfoTransfer(@NonNull PeerInfo peer) {
        this.accountServerSign = peer.getAccountServerSign();
        this.id = peer.getId();
        this.uid = peer.getUid();
        this.protocolVersion = peer.getProtocolVersion();
        this.displayName = peer.getDisplayName();
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(@NonNull String displayName) {
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getAccountServerSign() {
        return accountServerSign;
    }

    public void setAccountServerSign(String accountServerSign) {
        this.accountServerSign = accountServerSign;
    }
}
