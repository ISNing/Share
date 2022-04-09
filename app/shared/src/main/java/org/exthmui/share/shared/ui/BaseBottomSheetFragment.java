package org.exthmui.share.shared.ui;

import android.content.DialogInterface;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class BaseBottomSheetFragment extends BottomSheetDialogFragment {

  @Override
  public void dismiss() {
    super.dismiss();
    onCancelOrDismiss();
  }

  @Override
  public void dismissAllowingStateLoss() {
    super.dismissAllowingStateLoss();
    onCancelOrDismiss();
  }

  public void cancel() {
    dismiss();
    onCancelOrDismiss();
  }

  @Override
  public void onCancel(@NonNull DialogInterface dialog) {
    super.onCancel(dialog);
    onCancelOrDismiss();
  }

  public void onCancelOrDismiss() {
    requireActivity().finish();
  }
}
