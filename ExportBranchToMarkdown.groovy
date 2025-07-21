// @ExecutionModes({ON_SELECTED_NODE})

// author : Markus Seilnacht
// date : 2025-07-21
// (c) licensed under GPL-3.0 or later

/*
    This script exports a selected node and all nodes in this branch to Markdown.
    Text elements except headings are exported as they are..
    ..so be sure to work with Markdown in your notes.
    To export the whole mindmap just select the root.
    You can choose to create a TOC in your export.

    Be sure to permit operations (file/write) for Scripting-Plugin in Preferences !

    Attention :
    1. If you want to use 'Obsidian' set 'useObsidian' to true.
    2. The order of Markdown titles is equal to 'Outline view' (F10) in the mindmap.
    3. You can set the boolean values for exported elements to your own needs - they
        are used as defaults for the checkboxes.
*/

/* 
    #todo 01 : Links in Attributen
        Links die mit '/' oder '# ' beginnen, werden als Text exportiert !

    #todo 02 : Effizientere Nutzung von Pattern und Matcher ?

*/

import java.io.FileWriter
import java.io.IOException

import java.util.regex.Pattern
import java.util.regex.Matcher

import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JCheckBox

import java.awt.event.ItemListener
import java.awt.event.ItemEvent
import java.awt.GridLayout
import java.awt.Dimension


lf = System.lineSeparator()
// Indicator for being a header-line - they start with "#"
final String hIndicator = "#"

// Default for creating a TOC in Markdown file
boolean createTOC = true

// Use Obsidian to edit export
final boolean useObsidian = false

// exported elements
boolean expAlias = true
boolean expAttrs = true
boolean expConns = true
boolean expDetails = true
boolean expLinks = true
boolean expNotes = true
boolean expTags = true

// Pattern and Matcher for MD-Links
final Pattern mdPattern = Pattern.compile("\\[.+\\]\\(.+\\)")
final Matcher matcher = mdPattern.matcher("")

def sb = new StringBuffer()


/*
    method  : getPrecStr
    task    : create sequence of preceding characters
    input   : counter of needed chars, character for sequence
    return  : String with a sequence of charStr - e.g. for headers or TOC
*/
def String getPrecStr(int relLevel, String charStr) {
	String prec = ""
	for (i = 0; i < relLevel; i++) {
		prec += charStr
	}
	return prec
}


/*
    method  : addMetadata
    task    : write metadata to given StringBuffer
    input   : StringBuffer to write to
*/
def addMetadata(StringBuffer strBuff) {
    strBuff << "<!-- Begin Metadata  $lf"
    strBuff << "  $lf"
    strBuff << "[Freeplane file]:- '" << node.getMindMap().getFile().getPath() << "'  $lf"
    strBuff << "[Export date]:- '" << format(new Date(), "yyyy-MM-dd HH:mm:ss") << "'  $lf"
    strBuff << "[Export script]:- 'ExportBranchToMarkdown.groovy'  $lf"
    strBuff << "[Script author]:- 'GitHub/seimaku69; Markus Seilnacht; seimaku(at)proton(dot)me'  $lf"
    strBuff << "  $lf"
    strBuff << "End Metadata -->  $lf"
}


/*
    method  : getMDExportFile
    task    : asking for a file and overwrite
    return  : File to save Markdown export or null (aborted)
*/
def File getMdExportFile() {
    JFileChooser fChooser = ui.newFileChooser()
    fChooser.setDialogTitle("Export selected branch as Markdown to file..")
    FileNameExtensionFilter filter = new FileNameExtensionFilter("Markdown files", "md")
    fChooser.setFileFilter(filter);
    fName = node.text.strip().replace(" ","") + ".md"
    fChooser.setSelectedFile(new File(fName))

    File file = null
    while (!file) {
        int retVal = fChooser.showSaveDialog(ui.getFrame())
        if (retVal != JFileChooser.APPROVE_OPTION) return null
        file = fChooser.getSelectedFile()
        if (file.exists()) {
            int over = ui.showConfirmDialog(null, "Overwrite existing file with new export ?",
                "Markdown Export", JOptionPane.YES_NO_OPTION)
            if (over != JOptionPane.YES_OPTION) file = null
        }
    }
    return file
}


/*
    method  : getTOC
    task    : creating a table of contents
    input   : header Indicator, export-root Level
    return  : StringBuffer with TOC
*/
def StringBuffer getTOC(String hInd, int startNL) {
    def sbTOC = new StringBuffer()
    sbTOC << "  $lf" << getPrecStr(2, hInd) << " Table of Contents  $lf"
    sbTOC << "  $lf"
    for (n in node.findAll()) {
        nLevel = n.getNodeLevel(true)
        stripText = n.getShortText()
        lnk = stripText.toLowerCase().replace(" ", "-")
        sbTOC << getPrecStr(nLevel - startNL, "\t") << "- [$stripText](#$lnk)  $lf"
    }
    return sbTOC
}


// get a file for Markdown export
expFile = getMdExportFile()
if(!expFile) {
    c.setStatusInfo("standard", "Markdown export aborted ! ", "messagebox_warning")
    return
}

// choosing which elements to export (JPanel and ItemListener)
// -----------------------------------------------------

JPanel panel = new JPanel()
panel.setLayout(new GridLayout(7,1))

// Checkbox for Alias
ckAlias = new JCheckBox("Aliases", expAlias)
ckAlias.addItemListener(new ItemListener() {
	public void itemStateChanged(ItemEvent e) {
		expAlias = (e.getStateChange() == ItemEvent.SELECTED) ? true : false
	}
})
panel.add(ckAlias)

// Checkbox for attributes
ckAttrs = new JCheckBox("Attributes", expAttrs)
ckAttrs.addItemListener(new ItemListener() {
	public void itemStateChanged(ItemEvent e) {
		expAttrs = (e.getStateChange() == ItemEvent.SELECTED) ? true : false
	}
})
panel.add(ckAttrs)

// Checkbox for Connectors (in- and outgoing edges)
ckConns = new JCheckBox("Connectors", expConns)
ckConns.addItemListener(new ItemListener() {
	public void itemStateChanged(ItemEvent e) {
		expConns = (e.getStateChange() == ItemEvent.SELECTED) ? true : false
	}
})
panel.add(ckConns)

// Checkbox for details
ckDetails = new JCheckBox("Details", expDetails)
ckDetails.addItemListener(new ItemListener() {
	public void itemStateChanged(ItemEvent e) {
		expDetails = (e.getStateChange() == ItemEvent.SELECTED) ? true : false
	}
})
panel.add(ckDetails)

// Checkbox for links (core node links)
ckLinks = new JCheckBox("Links", expLinks)
ckLinks.addItemListener(new ItemListener() {
	public void itemStateChanged(ItemEvent e) {
		expLinks = (e.getStateChange() == ItemEvent.SELECTED) ? true : false
	}
})
panel.add(ckLinks)

// Checkbox for notes
ckNotes = new JCheckBox("Notes", expNotes)
ckNotes.addItemListener(new ItemListener() {
	public void itemStateChanged(ItemEvent e) {
		expNotes = (e.getStateChange() == ItemEvent.SELECTED) ? true : false
	}
})
panel.add(ckNotes)

// Checkbox for tags
ckTags = new JCheckBox("Tags", expTags)
ckTags.addItemListener(new ItemListener() {
	public void itemStateChanged(ItemEvent e) {
		expTags = (e.getStateChange() == ItemEvent.SELECTED) ? true : false
	}
})
panel.add(ckTags)
panel.setPreferredSize(new Dimension(200,200))
// show dialog to select elements
ret = JOptionPane.showConfirmDialog(ui.getFrame(), panel, "Export elements", JOptionPane.OK_CANCEL_OPTION)
if (ret == JOptionPane.CANCEL_OPTION) return

// -----------------------------------------------------

// asking for creation of TOC in Markdown file
int toc = ui.showConfirmDialog(null, "Do you want to create a TOC (table of contents) ?",
"Markdown Export", JOptionPane.YES_NO_OPTION)
if (toc == JOptionPane.YES_OPTION) createTOC = true

// level of selected node for export - global
startNodeLevel = node.getNodeLevel(true)

// adding metadata block
addMetadata(sb)

// Creating Markdown for each node in actual branch
node.findAll().each {
    // write header - don't touch user defined headers
    stripText = it.getShortText()
    if (it.getPlainText().startsWith(hIndicator)) {
        sb << "  $lf" << stripText << "  $lf"
    }
    else {
        sb << "  $lf" << getPrecStr(it.getNodeLevel(true) - startNodeLevel + 1, hIndicator) << " " << stripText << "  $lf"
    }
    // if node-text is a markdown-link - add link after header
    matcher.reset(it.getText())
    while (matcher.find()) {
	    sb << "  $lf" << "Link : " << matcher.group() << "  $lf"
    }
    
    // picture assigned to node
    if (it.getExternalObject()) {
        uri = it.getExternalObject().getUri()
        sb << "  $lf![$it.text]($uri)  $lf"
    }

    // alias of node
    if (expAlias && !it.getAlias().isEmpty()) {
        sb << "  $lf*Alias :* " << it.getAlias() << "  $lf"
    }

    // tags of node
    tagsList = it.getTags().getTags()
    if (expTags && tagsList.size() > 0) {
        sb << "  $lf*Tags :* " << tagsList.toString() << "  $lf"
    }

    // details of node
    if (expDetails && it.getDetails()) {
        sb << "  $lf*Details :*  $lf" << it.getDetails().toString() << "  $lf"
    }

    // handle attributes - written as code-block - formulas calculated (getValues)
    if (expAttrs && !it.attributes.empty) {
        attrs = it.getAttributes().getTransformed()
        sb << "  $lf*Attributes :*  $lf"
        sb << "  $lf"
        sb << "| Name | Value |$lf"
        sb << "| ---- | ----- |$lf"
        for (i = 0; i < attrs.size(); i++) {
            sb << "| " << attrs.getNames()[i] << " | " << attrs.getValues()[i] << " |" << lf
        }
    }

    // export Freeplane links (hyper, local hyper, website) as markdown
    if (expLinks && it.getLink()) {
        linkText = it.getLink().getText()
        outLnkText = "Unnamed_Link"
        outLnkUrl = linkText

        // link to another node - 'local hyperlink' - structure : #ID_...
        if (linkText.startsWith("#")) {
            linkID = linkText.replace("#", "")
            if (linkID) {
                linkedNode = it.getMindMap().node(linkID)
                urlText = linkedNode.getText().toLowerCase()
                urlText = urlText.replace(" ","-")
                outLnkUrl = "#".concat(urlText)
                outLnkText = linkedNode.getText()
            }
        }
        // link to a file - cutting 'file:'
        if (linkText.startsWith("file:")) {
            outLnkUrl = linkText.replace("file:", "")
            //outLnkText = it.getPlainText()
            linkedFile = new File(outLnkUrl)
            outLnkText = linkedFile.getName()
        }
        // relative link to a file with '../..' or '/..'
        if (linkText.startsWith("..") || linkText.startsWith("/")) {
            outLnkUrl = linkText
            //outLnkText = it.getPlainText()
            linkedFile = new File(outLnkUrl)
            outLnkText = linkedFile.getName()
        }
        // link to a website - link is correct in markdown
        if (linkText.startsWith("https:")) {
            outLnkUrl = linkText
            outLnkText = it.getPlainText()
        }
        sb <<"  $lf*Link :* [$outLnkText]($outLnkUrl)  $lf"
    }

    if (expConns) {
        it.getConnectorsOut().each { conn ->
            linkedNode = conn.getTarget()
            urlText = linkedNode.getText().toLowerCase()
            urlText = urlText.replace(" ", "-")
            outLnkUrl = "#".concat(urlText)
            outLnkText = linkedNode.getText()
            srcLabel = conn.getSourceLabel() ?: "n.a."
            midLabel = conn.getMiddleLabel() ?: "n.a."
            trgLabel = conn.getTargetLabel() ?: "n.a."
            sb << lf << "*..linked to :* [" << outLnkText << "]("
            sb << outLnkUrl << ") ; " << " Labesls{" << srcLabel << ", " 
                << midLabel << ", " << trgLabel << "}$lf"
        }

        it.getConnectorsIn().each { conn ->
            sourceNode = conn.getSource()    
            urlText = sourceNode.getText().toLowerCase()
            urlText = urlText.replace(" ", "-")
            srcLnkUrl = "#".concat(urlText)
            srcLnkText = sourceNode.getText()
            srcLabel = conn.getSourceLabel() ?: "n.a."
            midLabel = conn.getMiddleLabel() ?: "n.a."
            trgLabel = conn.getTargetLabel() ?: "n.a."
            sb << lf << "*..linked from :* [" << srcLnkText << "]("
            sb << srcLnkUrl << ") ; " << " Labesls{" << srcLabel << ", " 
                << midLabel << ", " << trgLabel << "}$lf"
        }
    }

    // copy note as paragraph text - should be markdown
    if (expNotes && it.getNote()) {
        sb << "  $lf" << it.getNote() << "  $lf"
    }

    // adding TOC after first title if chosen
    if (it == node && createTOC) sb << getTOC(hIndicator, startNodeLevel).toString()
}

// write export to choosen file
try {
    FileWriter myWriter = new FileWriter(expFile)
    myWriter.write(sb.toString())
    myWriter.close();
    c.setStatusInfo("standard", "Successfully exported to " + expFile.getPath(), "button_ok")
} catch (IOException ex) {
    ui.errorMessage("An error occurred ! " + ex)
    return
}
// write export to Root-Note
// node.mindMap.root.note = sb.toString()

// asking for opening exported file with associated (to markdown) application or Obsidian
int openExp = ui.showConfirmDialog(null, "Export to " + expFile.getPath() + " successful ! $lf"
    + "Do you want to open this file ?","Markdown Export", JOptionPane.YES_NO_OPTION)
if (openExp == JOptionPane.YES_OPTION) {
    prefix = ""
    if (useObsidian) prefix = "obsidian://open?path="
    expUri = java.net.URI.create(prefix + expFile.getPath())
    loadUri(expUri)
}
