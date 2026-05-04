package com.Team2_CDE_master.ProjectServer.crdt;

public class FormattingOperation {
    CharID targetId;
    String formatType;
    boolean formatValue;

    public FormattingOperation(CharID targetId, String formatType, boolean formatValue) {
        this.targetId = targetId;
        this.formatType = formatType;
        this.formatValue = formatValue;
    }
    public void apply(CharCRDT doc) {
        doc.applyFormatting(targetId, formatType, formatValue);
    }
    public CharID getTargetId() { return targetId; }
    public String getFormatType() { return formatType; }
    public boolean getFormatValue() { return formatValue; }
}
