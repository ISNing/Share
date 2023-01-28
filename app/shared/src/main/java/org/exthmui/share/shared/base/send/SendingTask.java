package org.exthmui.share.shared.base.send;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import org.exthmui.share.shared.base.BaseTask;
import org.exthmui.share.shared.base.BaseWorker;
import org.exthmui.share.shared.base.Entity;
import org.exthmui.share.shared.exceptions.trans.InvalidInputDataException;
import org.exthmui.share.taskMgr.Result;
import org.exthmui.share.taskMgr.entities.TaskEntity;

public abstract class SendingTask extends BaseTask {

    public SendingTask(@NonNull Context context, String taskId, Bundle inputData) {
        super(context, taskId, inputData);
    }

    public SendingTask(@NonNull TaskEntity taskEntity) {
        super(taskEntity);
    }

    /**
     * InputData:
     * MUST contain:{@link String[]}: {@link Entity#ENTITIES} (Parceled, Encoded in Base64),
     * {@link String[]}: {@link Sender#TARGET_PEER_ID}
     * {@link String}: {@link Sender#TARGET_PEER_NAME}
     * ***** Extra values is allowed *****
     *
     * @return Result of work.
     * @see BaseWorker#doWork()
     */
    @NonNull
    @Override
    public final Result doWork() {
        Bundle input = getInputData();
        String[] entitiesStrings = input.getStringArray(Entity.ENTITIES);
        if (entitiesStrings == null || entitiesStrings.length == 0)
            return genFailureResult(new InvalidInputDataException(getApplicationContext()), null);

        Entity[] entities = Entity.stringsToEntities(entitiesStrings);
        String peerId = input.getString(Sender.TARGET_PEER_ID);
        String peerName = input.getString(Sender.TARGET_PEER_NAME);
        return doWork(entities, peerId, peerName);
    }

    @NonNull
    public abstract Result doWork(Entity[] entities, String peerId, String peerName);
}
