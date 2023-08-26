package org.exthmui.share.taskMgr.events

import android.os.Bundle
import java.util.EventObject

class ProgressUpdatedEvent(source: Any?, val progress: Bundle) : EventObject(source)
