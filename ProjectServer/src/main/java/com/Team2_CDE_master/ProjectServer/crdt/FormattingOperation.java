package com.Team2_CDE_master.ProjectServer.crdt;

// this class represents one formatting operation: bold or italic
// basically i give it a target char id, what type of format, and true/false
// then call apply() to actually do it on the document
public class FormattingOperation {

    // the id of the character we want to format
    CharID targetId;

    // what kind of formatting: "bold" or "italic"
    String formatType;

    // true = turn it on, false = turn it off
    boolean formatValue;

    public FormattingOperation(CharID targetId, String formatType, boolean formatValue) {
        this.targetId = targetId;
        this.formatType = formatType;
        this.formatValue = formatValue;
    }


    // apply formatting
    public void apply(CharCRDT doc) {
        doc.applyFormatting(targetId, formatType, formatValue);
    }

    // getters
    public CharID getTargetId() { return targetId; }
    public String getFormatType() { return formatType; }
    public boolean getFormatValue() { return formatValue; }
}