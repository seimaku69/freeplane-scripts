// @ExecutionModes({ON_SELECTED_NODE})

// author : Markus Seilnacht
// date : 2025-07-24
// (c) licensed under GPL-3.0 or later


/* 
    This script lists all tasks (tType == tTask) in the subtree of a node which are not 
    done (tStatus != tDone) or have no tStatus attribute.
    It creates a child for every undone task with a direct link to it.
    It is recommended to set values for all variables (tType,tTask,tStatus,tDone) to your
    own way of thinking.
*/

final String lf = System.lineSeparator()

// defines which attribute is signed as task
final String tType = "TYPE"
final String tTask = "TASK"
// defines which attribute is checked for status and Done-Value
final String tStatus = "STATUS"
final String tDone = "DONE"

def StringBuilder sb = new StringBuilder()

// create new child node from actual selected
taskNode = node.createChild()
taskNode.setFormat("markdownPatternFormat")
taskNode.setNoteContentType("auto")
taskNode.setFree(true)
taskNode.style.setBackgroundColorCode("#FFFF00")
taskNode.style.setTextColorCode("#333333")
// set attribute to actual date
taskNode.text = "**Tasklist (All Tasks) " + '**'
taskNode.attributes = ["Date" : format(new Date(), "yyyy-MM-dd : HH:mm")]
taskNode.attributes.optimizeWidths()

sb << "Tasklist (All Tasks)" << lf
sb << lf
sb << "The childs of this node are all successors of [$node.text] with attributes :" << lf
sb << lf
sb << "$tType  == $tTask && $tStatus != $tDone" << lf
sb << lf
sb << "They provide a direct link to each task.." << lf
// scan successors of actual node - taskNode included in findAll()
for (child in node.findAll()) {
    isTask = (child.attributes.getFirst(tType) == tTask) ? true : false
    status = child.attributes.getTransformed().getFirst(tStatus)
    if (!isTask || status == tDone) continue
    // Create child with link for each open task in original color
    newChild = taskNode.createChild()
    newChild.style.setBackgroundColorCode(child.style.getBackgroundColorCode())
    newChild.style.setTextColorCode(child.style.getTextColorCode())
    newChild.format = "markdownPatternFormat"
    newChild.setNoteContentType("auto")
    newChild.text = "**$child.text**"
    newChild.link.text = "#$child.id"
}
// add attribute with formula
taskNode.attributes.add("Task(s)", "=children.size()")
taskNode.note = sb.toString()
taskNode.setFolded(true)
c.select(taskNode)


