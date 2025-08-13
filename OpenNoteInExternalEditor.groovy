// @ExecutionModes({ON_SELECTED_NODE})

// author : Markus Seilnacht
// date : 2025-08-10
// (c) licensed under GPL-3.0 or later

/*
This script opens the note of a selected node in an external editor with a new generated 
temporary file. After closing this editor, the new content is written to node and
the minmap will be saved.
If you don't save the temporary file in your editor there are no changes in your note.

    Attention :
    1. Don't use an existing instance of any editor to edit the temporary file. 
        This script waits for the termination of the process - otherwise it fails..
    2. It is recommended to set the value of 'editor' to your own preferences.
    3. The external editor is opened in modal-mode - means Freeplane isn't available 
        before closing it.
    
    Tips : 
        1. You can assign a shortcut to this script in Freeplane to make access easier !
        2. Set configuration of your editor to show white-spaces - so you can see newlines.
        3. If problems occur with your editor try the commented lines for 'tmpPath' and 'tmpFile' 
*/

/* 
    #todo 01 : Notizen nicht modal öffnen ?!
        Dieses Script öffnet Notizen aktuell in einem modale Modus.
        Sollte es möglich sein dies nicht-modal zu tun ? Anwendungen ? Änderungen speichern ?
*/

import java.io.File
import groovy.json.JsonSlurper

// String editor = "/usr/bin/xed --new-window"
String editor = "/usr/bin/ghostwriter"    // free, fast,with preview
// String editor = "/usr/bin/notepadqq"

// search config for editor - overwrites setting above if exists
String userDir = c.getUserDirectory().getPath()
File configFile = new File(userDir + File.separator + "scriptConfig.json")

if (configFile.exists()) {
    JsonSlurper jsSlurper = new JsonSlurper()
	Map jsMap = jsSlurper.parse(configFile)
    if (jsMap['extern.editor.note']) editor = jsMap['extern.editor.note']
}

// check content type of note to set extension 
// extensions determines often syntax highlighting in an editor
switch (node.getNoteContentType()) {
    case "markdown":    // ContentType : Markdown
        ext = ".md"
        break
    case "html":        // ContentType : Text / HTML
        ext = ".html"
        break
    case "latex":       // ContentType : Latex
        ext = ".latex"
        break
    default:            // ContentType : Standard (auto)
        ext = ".txt"
}

File tmpPath = node.getMindMap().getFile()
if (!tmpPath) {
    ui.errorMessage("No file exists for this map - save it first ?!")
    return
}
tmpPath = tmpPath.getParentFile()

//create temporary file
try {
    tmpFile = File.createTempFile('~' + node.id + ":", ext, tmpPath) 
    String text = node.note ?: ""
    tmpFile.setText(text, 'UTF-8')
} catch (IOException e) {
    ui.errorMessage("Can not create temporary file !")
    return
}

String exCmd = editor + " " + tmpFile.getAbsolutePath()
Process process = exCmd.execute()
process.waitFor()

// read new content and save
node.note = tmpFile.getText('UTF-8')
node.getMindMap().save(true)
// delete temporary file
tmpFile.delete()
