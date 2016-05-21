package controllers

import shared.WorkItem

object WorkQueue {
  private val workItems = collection.mutable.Queue(
    WorkItem(0, "alert(\"woot!\""),
    WorkItem(1, "console.log(\"processed item 1\")"),
    WorkItem(2, "console.log(\"processed item 2\")"),
    WorkItem(3, "console.log(\"processed item 3\")"),
    WorkItem(4, "console.log(\"processed item 4\")")
  )


  def dequeue(): WorkItem = workItems.dequeue()
}

