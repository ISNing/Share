package org.exthmui.share.shared.ui;

import android.app.PendingIntent;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.exthmui.share.shared.R;
import org.exthmui.share.shared.ReceiverUtils;

public class AcceptationRequestBottomSheetFragment extends BottomSheetDialogFragment {

    public static final String TAG = "AcceptationRequestBottomSheetFragment";

    private final String mPluginCode;
    private final String mPeerName;
    private final String mFileName;
    private final long mFileSize;
    private final String mRequestId;
    private final int mNotificationId;

    private TextView mTitle;
    private TextView mSize;
    private Button mAcceptButton;
    private Button mRejectButton;

    public AcceptationRequestBottomSheetFragment(String pluginCode, String peerName, String fileName, long fileSize, String requestId, int notificationId) {
        this.mPluginCode = pluginCode;
        this.mPeerName = peerName;
        this.mFileName = fileName;
        this.mFileSize = fileSize;
        this.mRequestId = requestId;
        this.mNotificationId = notificationId;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_acceptation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTitle = view.findViewById(R.id.fragment_acceptation_title);
        mSize = view.findViewById(R.id.fragment_acceptation_size);
        mAcceptButton = view.findViewById(R.id.fragment_acceptation_accept_button);
        mRejectButton = view.findViewById(R.id.fragment_acceptation_reject_button);

        mTitle.setText(requireContext().getString(R.string.dialog_title_accept_or_reject_request, mPeerName, mFileName));
        mSize.setText(requireContext().getString(R.string.dialog_accept_or_reject_request_size, Formatter.formatFileSize(requireContext(), mFileSize)));

        mAcceptButton.setOnClickListener(v -> {
            PendingIntent pendingIntent = ReceiverUtils.buildAcceptPendingIntent(requireContext(), mPluginCode, mRequestId, mNotificationId);
            try {
                pendingIntent.send();
                requireActivity().finish();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        });

        mRejectButton.setOnClickListener(v -> {
            PendingIntent pendingIntent = ReceiverUtils.buildRejectPendingIntent(requireContext(), mPluginCode, mRequestId, mNotificationId);
            try {
                pendingIntent.send();
                requireActivity().finish();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        });
    }

    public void onCancel() {
        dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        requireActivity().finish();
    }
}