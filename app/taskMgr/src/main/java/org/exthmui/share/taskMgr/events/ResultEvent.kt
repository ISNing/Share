package org.exthmui.share.taskMgr.events

import org.exthmui.share.taskMgr.Result
import java.util.EventObject

class ResultEvent(source: Any?, val result: Result) : EventObject(source)
