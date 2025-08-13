// @ExecutionModes({ON_SINGLE_NODE})

// author : Markus Seilnacht
// date : 2025-08-10
// (c) licensed under GPL-3.0 or later

/*
    This script opens an external editor with a temporary generated script file.
    The temporary script file is located in user directory.
    (e.g. '~/.config/freeplane/[version]/tmpSrcipt.groovy)
    In 'ExecuteScriptInEditor' the same file have to be used !

    ATTENTION : 
    1. The editor in this script can't be the same instance as the one to edit note-texts. 
    This one is not opened modal and if they are used in one instance the note change is 
    not recognized and sved in the right way.

    2. It is recommended to edit the value of 'editor' for your own needs.
*/

import groovy.json.JsonSlurper

// setup for editor to use
// String editor = "/usr/bin/xed"
String editor = "flatpak run org.geany.Geany"  // flatpak :-/
// String editor = "/usr/bin/flatpak run --branch=stable --arch=x86_64 --command=kate --file-forwarding org.kde.kate"
// String editor = "/usr/bin/notepadqq --new-window"

// search config for editor - overwrites setting above if exists
String userDir = c.getUserDirectory().getPath()
File configFile = new File(userDir + File.separator + "scriptConfig.json")

if (configFile.exists()) {
    JsonSlurper jsSlurper = new JsonSlurper()
	Map jsMap = jsSlurper.parse(configFile)
    if (jsMap['extern.editor.script']) editor = jsMap['extern.editor.script']
}

// path to temporary script file - e.g. '~/.config/freeplane/[version]/tmpScript.groovy
String tmpPath = c.getUserDirectory().getAbsolutePath() + File.separator + "tmpScript.groovy"

// create temporary file
// using existing file ? ..otherwise delete it with tmpFile.delete()
try {
    tmpFile = new File(tmpPath)
    if (!tmpFile.exists()) {
        tmpFile.createNewFile()
    }
} catch (IOException e) {
    ui.errorMessage("Can not create temporary file !")
    return
}

// open editor and don't wait for closing
// ..running the script is triggered by 'ExecuteScriptInEditor'
exCmd = editor + " " + tmpPath
Process process = exCmd.execute()
