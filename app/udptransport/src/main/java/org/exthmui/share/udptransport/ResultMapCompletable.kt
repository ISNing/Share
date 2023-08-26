package org.exthmui.share.udptransport

import android.content.Context
import android.util.Log
import org.exthmui.share.shared.base.results.BasicTransmissionResult
import org.exthmui.share.shared.base.results.SuccessTransmissionResult
import org.exthmui.share.shared.base.results.TransmissionResult
import org.exthmui.share.shared.exceptions.trans.CancelledException
import org.exthmui.share.shared.exceptions.trans.ReceiverCancelledException
import org.exthmui.share.shared.exceptions.trans.SenderCancelledException
import org.exthmui.share.shared.exceptions.trans.TransmissionException
import org.exthmui.share.shared.exceptions.trans.UnknownErrorException

abstract class ResultMapCompletable(val context: Context) {
    protected val mutableResultMap: MutableMap<String, TransmissionResult> = HashMap()
    private val resultMap: Map<String, TransmissionResult>
        get() = mutableResultMap.toMap()
    protected abstract var isSenderCanceled: Boolean
    protected abstract var isReceiverCanceled: Boolean

    protected fun completeRestWith(result: TransmissionResult): Pair<TransmissionResult, Map<String, TransmissionResult>> {
        setRestAs(result)
        return Pair(result, resultMap)
    }

    protected fun setRestAs(result: TransmissionResult) {
        Utils.replaceMapValue(mutableResultMap, BasicTransmissionResult.UNKNOWN_RESULT, result)
    }

    protected fun complete(result: TransmissionResult): Pair<TransmissionResult, Map<String, TransmissionResult>> {
        return Pair(result, resultMap)
    }

    protected fun complete(): Pair<TransmissionResult, Map<String, TransmissionResult>> =
        complete(resultMap.values.filterIsInstance<TransmissionException>().let {
            if (it.isEmpty()) SuccessTransmissionResult(context)
            else {
                var list = it
                // Only consider errors that are not CancelledException if they exists
                list.filter { it !is CancelledException }.takeIf { !it.isEmpty() }
                    ?.apply { list = this }
                try {
                    val res = Utils.getCommonBaseClass(list).getConstructor(Context::class.java)
                        .newInstance(context)
                    // If res is not TransmissionException's subclass(include itself), convert it to a UnknownError
                    if (res is TransmissionException && !TransmissionException::class.java.isInstance(
                            res
                        )
                    ) {
                        res
                    } else UnknownErrorException(context, res as TransmissionException)
                } catch (e: Exception) {
                    UnknownErrorException(context)
                }
            }
        })

    fun isCanceled(): Boolean = isSenderCanceled || isReceiverCanceled
    fun checkCanceledSingleResult(): TransmissionResult? {
        return if (isSenderCanceled) {
            Log.d(UDPReceiver.TAG, "Transfering canceled by sender")
            SenderCancelledException(context)
        } else if (isReceiverCanceled) {
            Log.d(UDPReceiver.TAG, "Transfering canceled by receiver")
            ReceiverCancelledException(context)
        } else null
    }

    fun checkCanceledAndComplete(): Pair<TransmissionResult, Map<String, TransmissionResult>>? {
        return checkCanceledSingleResult()?.let {
            completeRestWith(it)
        }
    }
}