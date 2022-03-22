package org.exthmui.share.shared.ui;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.exthmui.share.shared.R;
import org.exthmui.share.shared.ReceiverUtils;
import org.exthmui.share.shared.databinding.FragmentAcceptationBinding;

public class AcceptationRequestBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String TAG = "AcceptationRequestBottomSheetFragment";

    FragmentAcceptationBinding binding;

    private final String mPluginCode;
    private final String mPeerName;
    private final String mFileName;
    private final long mFileSize;
    private final String mRequestId;
    private final int mNotificationId;

    public AcceptationRequestBottomSheetFragment(String pluginCode, String peerName, String fileName, long fileSize, String requestId, int notificationId) {
        this.mPluginCode = pluginCode;
        this.mPeerName = peerName;
        this.mFileName = fileName;
        this.mFileSize = fileSize;
        this.mRequestId = requestId;
        this.mNotificationId = notificationId;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAcceptationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.fragmentAcceptationTitle.setText(String.format(requireContext().getString(R.string.dialog_title_accept_or_reject_request), mPeerName, mFileName));
        binding.fragmentAcceptationSize.setText(String.format(requireContext().getString(R.string.dialog_accept_or_reject_request_size), Formatter.formatFileSize(requireContext(), mFileSize)));

        binding.fragmentAcceptationAcceptButton.setOnClickListener(v -> {
            PendingIntent pendingIntent = ReceiverUtils.buildAcceptPendingIntent(requireContext(), mPluginCode, mRequestId, mNotificationId);
            try {
                pendingIntent.send();
                requireActivity().finish();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        });

        binding.fragmentAcceptationRejectButton.setOnClickListener(v -> {
            PendingIntent pendingIntent = ReceiverUtils.buildRejectPendingIntent(requireContext(), mPluginCode, mRequestId, mNotificationId);
            try {
                pendingIntent.send();
                requireActivity().finish();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        requireActivity().finish();
    }
}