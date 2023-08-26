package org.exthmui.share.shared.base.send

import android.content.Context
import android.os.Bundle
import org.exthmui.share.shared.base.BaseTask
import org.exthmui.share.shared.base.Entity
import org.exthmui.share.shared.exceptions.trans.InvalidInputDataException
import org.exthmui.share.taskMgr.Result
import org.exthmui.share.taskMgr.entities.TaskEntity

abstract class SendingTask : BaseTask {
    constructor(context: Context, inputData: Bundle?) : super(context, inputData)
    constructor(taskEntity: TaskEntity) : super(taskEntity)

    /**
     * InputData:
     * MUST contain:[String[]]: [Entity.ENTITIES] (Parceled, Encoded in Base64),
     * [String[]]: [Sender.TARGET_PEER_ID]
     * [String]: [Sender.TARGET_PEER_NAME]
     * ***** Extra values is allowed *****
     *
     * @return Result of work.
     * @see BaseTask.doWork
     */
    override suspend fun doWork(): Result {
        val input = inputData
        val entitiesStrings = input.getStringArray(Entity.ENTITIES)
        if (entitiesStrings.isNullOrEmpty()) return genFailureResult(
            InvalidInputDataException(
                applicationContext!!
            ), null
        )
        val entities = Entity.stringsToEntities(entitiesStrings)
        val peerId = input.getString(Sender.TARGET_PEER_ID)!!
        val peerName = input.getString(Sender.TARGET_PEER_NAME)!!
        return doWork(entities, peerId, peerName)
    }

    abstract suspend fun doWork(entities: Array<Entity>, peerId: String, peerName: String): Result
}
