// @ExecutionModes({ON_SELECTED_NODE})

// author : Markus Seilnacht
// date : 2025-08-13
// (c) licensed under GPL-3.0 or later


/* 
    This script lists all tasks (tType == tTask) in the subtree of a node which are not 
    done (tStatus != tDone) or have no tStatus attribute.
    It creates a child for every undone task with a direct link to it.
    It is recommended to set values for all variables (tType,tTask,tStatus,tDone) to your
    own way of thinking.
*/

/*
    todo 01 : Umstellung auf externe Konfiguration
*/

import groovy.json.JsonSlurper

final String lf = System.lineSeparator()

// defines which attribute is signed as task
String tType = "TYPE"
String tTask = "TASK"
// defines which attribute is checked for status and Done-Value
String tStatus = "STATUS"
String tDone = "DONE"

def StringBuilder sb = new StringBuilder()


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
    }
}

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
