// @ExecutionModes({ON_SELECTED_NODE})

// author : Markus Seilnacht
// date : 2025-07-01
// (c) licensed under GPL-3.0 or later


/* 
    This script lists all tasks (tType == tTask) in the subtree of a node which are not 
    done (tStatus != tDone) or have no tStatus attribute.
    It creates a child of the Tasklist with a direct link to undone task.
    It is recommended to set adjust values for the used variables (tType,tTask,tStatus,tDone)
    to meet your needs.
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
node.findAll().each {
    if (it.attributes.getFirst(tType) == tTask) {
        // create child with link for each open task or task without status
        if (it.attributes.getTransformed().getFirst(tStatus) != tDone) {
            // Create child with a link for each open task
            // Each child gets the colors of its task
            newChild = taskNode.createChild()
            newChild.style.setBackgroundColorCode(it.style.getBackgroundColorCode())
            newChild.style.setTextColorCode(it.style.getTextColorCode())
            newChild.format = "markdownPatternFormat"
            newChild.setNoteContentType("auto")
            newChild.text = "**$it.text**"
            newChild.link.text = "#$it.id"
        }
    }    
}

// add attribute with formula
taskNode.attributes.add("Task(s)", "=children.size()")

// write to note of task-node
taskNode.note = sb.toString()
taskNode.setFolded(true)
// select node
c.select(taskNode)


