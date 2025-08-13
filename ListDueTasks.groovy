// @ExecutionModes({ON_SELECTED_NODE})

// author : Markus Seilnacht
// date : 2025-08-13
// (c) licensed under GPL-3.0 or later


/*
    This script finds all tasks (atribute tType == tTask) which are due today or former.
    if they have an attribute tStatus == tDone they are not listed.
    ..or use optional scriptConfig.json.
    It creates a child with direct link to each found task.

    Attention : The value for 'DUE DATE' (tDueDate) have to follow ISO-8601 ('yyyy-MM-dd')
*/

/*
    #todo 01 : Behandlung von parse-Errors für anderes Datumsformat

    #todo 03 : Formel im Attribut des 'taskNode'
*/

import java.time.LocalDate
import groovy.json.JsonSlurper

final lf = System.lineSeparator()

def sb = new StringBuilder()

// defines which attribute is signed as task
String tType = "TYPE"
String tTask = "TASK"
// defines which attribute is checked for status and Done-Value
String tStatus = "STATUS"
String tDone = "DONE"
// checks this attribute against today
String tDueDate = "DUE DATE"

LocalDate now = LocalDate.now()


// Path for config file - overwrites setting above if exists
final String userDir = c.getUserDirectory().getPath()
final File configFile = new File(userDir + File.separator + "scriptConfig.json")

// Search for external configuration parameters - overwrite internal ones !
if (configFile.exists()) {
    JsonSlurper jsSlurper = new JsonSlurper()
	Map jsMap = jsSlurper.parse(configFile)
    tasks = jsMap['tasks.params']
    if (tasks) {
        if (tasks['type.identifier']) tType = tasks['type.identifier']
        if (tasks['type.task']) tTask = tasks['type.task']
        if (tasks['status.identifier']) tStatus = tasks['status.identifier']
        if (tasks['status.done']) tDone = tasks['status.done']
        if (tasks['duedate.identifier']) tDueDate = tasks['duedate.identifier']
    }
}

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
