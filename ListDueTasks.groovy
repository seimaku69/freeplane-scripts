// @ExecutionModes({ON_SELECTED_NODE})

// author : Markus Seilnacht
// date : 2025-07-24
// (c) licensed under GPL-3.0 or later


/*
    This script finds all tasks (atribute tType == tTask) which are due today or former.
    if they have an attribute tStatus == tDone they are not listed.
    It creates a child with direct link to each found task.

    Attention : The value for 'DUE DATE' (tDueDate) have to follow ISO-8601 ('yyyy-MM-dd')
*/

/*
    #todo 01 : Behandlung von parse-Errors für anderes Datumsformat

    #todo 02 : Ausgabe analog zu 'ListAllTasks'

    #todo 03 : Formel im Attribut des 'taskNode'
*/

import java.time.LocalDate

final lf = System.lineSeparator()

def sb = new StringBuilder()

// defines which attribute is signed as task
final String tType = "TYPE"
final String tTask = "TASK"
// defines which attribute is checked for status and Done-Value
final String tStatus = "STATUS"
final String tDone = "DONE"
// checks this attribute against today
final tDueDate = "DUE DATE"

LocalDate now = LocalDate.now()

// create new child node from actual selected
taskNode = node.createChild()
taskNode.setFormat("markdownPatternFormat")
taskNode.setNoteContentType("auto")
taskNode.setFree(true)
taskNode.style.setBackgroundColorCode("#FFFF00")
taskNode.style.setTextColorCode("#333333")

// set timestamp
taskNode.text = "**Tasklist (Due Tasks) " + "**"
taskNode.attributes = ["Date" : format(new Date(), "yyyy-MM-dd  HH:mm:ss")]
taskNode.attributes.optimizeWidths()

sb << "Tasklist (Due Tasks)" << lf
sb << lf
sb << "The childs of this node are all successors of [$node.text] with attributes :" << lf
sb << lf
sb << "$tType  == $tTask && $tStatus != $tDone && $tDueDate <= " << now.toString() << lf
sb << lf
sb << "They provide a direct link to each task.." << lf

taskNode.attributes = ["Date" : format(new Date(), "yyyy-MM-dd  HH:mm:ss")]

for (child in node.findAll()) {
    dueVal = child.attributes.getTransformed().getFirst(tDueDate)
    isTask = (child.attributes.getFirst(tType) == tTask) ? true : false
    status = child.attributes.getTransformed().getFirst(tStatus)
    if (!dueVal || !isTask || status == tDone) continue
    dueDate = LocalDate.parse(dueVal)

    if (dueDate.compareTo(now) <= 0) {
        // Create child with link for each open task in original color
        newChild = taskNode.createChild()
        newChild.style.setBackgroundColorCode(child.style.getBackgroundColorCode())
        newChild.style.setTextColorCode(child.style.getTextColorCode())
        newChild.format = "markdownPatternFormat"
        newChild.setNoteContentType("auto")
        newChild.text = "**$child.text**"
        newChild.link.text = "#$child.id"
    }
}

// add attribute with formula
taskNode.attributes.add("Task(s)", "=children.size()")
taskNode.note = sb.toString()
taskNode.setFolded(true)
c.select(taskNode)