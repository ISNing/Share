package org.exthmui.share.taskMgr

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.exthmui.share.taskMgr.entities.TaskEntity
import org.exthmui.share.taskMgr.events.ProgressUpdatedEvent
import org.exthmui.share.taskMgr.events.ResultEvent
import org.exthmui.share.taskMgr.listeners.OnProgressUpdatedListener
import org.exthmui.share.taskMgr.listeners.OnResultListener
import org.exthmui.utils.BaseEventListenersUtils
import org.exthmui.utils.listeners.BaseEventListener
import java.lang.reflect.InvocationTargetException
import java.util.EventObject
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

abstract class Task : CancelableSuspendableRunnable {
    val taskId: String
    var status: TaskStatus
    val inputData: Bundle
    private val mResultFuture = InternalCancelCompletableFuture<Result>()

    @Volatile
    var progressData: Bundle? = null
        private set
    val progressDataLiveData = MutableLiveData(
        progressData
    )
    private val mListeners: MutableCollection<BaseEventListener> = HashSet()
    private fun notifyListeners(event: EventObject) {
        BaseEventListenersUtils.notifyListeners(event, mListeners)
    }

    fun registerListener(listener: BaseEventListener) {
        if (BaseEventListenersUtils.isThisListenerSuitable(
                listener,
                LISTENER_TYPES_ALLOWED
            )
        ) mListeners.add(listener)
    }

    fun unregisterListener(listener: BaseEventListener) {
        mListeners.remove(listener)
    }

    constructor(taskId: String, inputData: Bundle?) {
        this.taskId = taskId
        this.inputData = inputData ?: Bundle.EMPTY
        status = TaskStatus.CREATED
    }

    constructor(taskEntity: TaskEntity) {
        taskId = taskEntity.taskId
        val input = taskEntity.inputData
        inputData = input
        status = taskEntity.status
        // Result will be loaded by TaskManager
    }

    protected fun setResult(result: Result) {
        mResultFuture.complete(result)
        status = when (result.status) {
            Result.Status.SUCCESS -> TaskStatus.SUCCESS
            Result.Status.ERROR -> TaskStatus.ERROR
            Result.Status.CANCELLED -> TaskStatus.CANCELLED
        }
        notifyListeners(ResultEvent(this, result))
    }

    var deferred: Deferred<Result>? = null
    val resultFuture: Future<Result>
        get() = mResultFuture
    val isCancelled: Boolean
        get() = status === TaskStatus.CANCELLED

    @OptIn(InternalCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    override suspend fun runSuspendable(): Unit = coroutineScope {
        deferred = async {
            if (isCancelled) Result.cancelled()
            else
                try {
                    doWorkDelegate()
                } catch (error: CancellationException) {
                    Result.cancelled()
                } catch (error: Throwable) {
                    Result.failure(error)
                }
        }

        deferred?.invokeOnCompletion(true, true) {
            deferred?.let { setResult(it.getCompleted()) }
        }
    }

    override fun cancel() {
        status = TaskStatus.CANCELLED
        mResultFuture.internalCancel(true)
        deferred?.cancel()
    }

    protected fun updateProgress(progressData: Bundle?) {
        this.progressData = progressData
        progressDataLiveData.postValue(progressData)
        notifyListeners(ProgressUpdatedEvent(this, progressData!!))
    }

    abstract suspend fun doWorkDelegate(): Result

    private class InternalCancelCompletableFuture<T> : CompletableFuture<T>() {
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            // Refuse to cancel the future outside the class
            return false
        }

        internal fun internalCancel(mayInterruptIfRunning: Boolean) {
            super.cancel(mayInterruptIfRunning)
        }
    }

    companion object {
        const val TAG = "Task"
        private val LISTENER_TYPES_ALLOWED = arrayOf<Class<*>>(
            OnProgressUpdatedListener::class.java,
            OnResultListener::class.java
        ) as Array<Class<out BaseEventListener>>

        fun load(database: TaskDatabase, id: String?): Task? {
            var task: Task? = null
            database.taskDao().getTaskEntityById(id)?.let {
                try {
                    task = Class.forName(it.taskType).getConstructor(TaskEntity::class.java)
                        .newInstance(it) as Task
                    val result = Result.load(database, it.resultId)
                    task?.let {
                        it.mResultFuture.complete(result)
                        Log.d(
                            TAG,
                            String.format("Task %s(Id:%s) loaded", it.javaClass.name, it.taskId)
                        )
                    }
                } catch (e: IllegalAccessException) {
                    Log.e(TAG, String.format("Failed instancing task %s from database: %s", id, e))
                    e.printStackTrace()
                } catch (e: InstantiationException) {
                    Log.e(TAG, String.format("Failed instancing task %s from database: %s", id, e))
                    e.printStackTrace()
                } catch (e: InvocationTargetException) {
                    Log.e(TAG, String.format("Failed instancing task %s from database: %s", id, e))
                    e.printStackTrace()
                } catch (e: NoSuchMethodException) {
                    Log.e(TAG, String.format("Failed instancing task %s from database: %s", id, e))
                    e.printStackTrace()
                } catch (e: ClassNotFoundException) {
                    Log.e(TAG, String.format("Failed instancing task %s from database: %s", id, e))
                    e.printStackTrace()
                }
            }
            return task
        }
    }
}