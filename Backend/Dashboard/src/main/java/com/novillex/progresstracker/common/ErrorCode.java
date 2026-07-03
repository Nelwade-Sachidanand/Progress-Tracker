package com.novillex.progresstracker.common;

public class ErrorCode {

    private ErrorCode() {
    }

    // Validation Errors
    public static final String PROJECT_NAME_REQUIRED = "VAL_001";
    public static final String PHASE_NAME_REQUIRED = "VAL_002";
    public static final String MILESTONE_REQUIRED = "VAL_003";
    public static final String TASK_REQUIRED = "VAL_004";
    public static final String SUBTASK_REQUIRED = "VAL_005";

    public static final String ACTIVITY_ALREADY_EXISTS = "VAL_006";
    public static final String INVALID_PLANNED_DATES = "VAL_007";
    public static final String INVALID_ACTUAL_DATES = "VAL_008";
    public static final String PLANNED_DATE_REQUIRED = "VAL_009";
    public static final String ACTUAL_START_REQUIRED = "VAL_010";
    public static final String INVALID_PROGRESS = "VAL_011";
    public static final String ESTIMATED_PERIOD_INVALID = "VAL_012";
    public static final String USERNAME_REQUIRED = "VAL_013";
    public static final String NO_CHANGES_FOUND = "VAL_014";
    public static final String ACTIVITY_NAME_REQUIRED = "VAL_015";
	public static final String REQUEST_ALREADY_PENDING = "VAL_016";
	public static final String PROJECT_ID_REQUIRED = "VAL_017";
	public static final String PROJECT_ALREADY_EXISTS = "VAL_018";
	public static final String INVALID_REQUEST = "INV_019";
	public static final String INVALID_PASSWORD = "VAL_020";
	public static final String PASSWORD_MISMATCH = "ERR-043";
	public static final String PASSWORD_ALREADY_USED = "ERR-044";
	public static final String ACTIVITY_LOCKED = "VAL_21";




    // Resource Not Found
    public static final String PROJECT_NOT_FOUND = "PRJ_404";
    public static final String PHASE_NOT_FOUND = "PH_404";
    public static final String MILESTONE_NOT_FOUND = "MIL_404";
    public static final String TASK_NOT_FOUND = "TAS_404";
    public static final String SUBTASK_NOT_FOUND = "SUB_404";
    public static final String ACTIVITY_NOT_FOUND = "ACT_404";
    public static final String USER_NOT_FOUND = "USR_404";
    public static final String AUDIT_NOT_FOUND = "AUD_404";
	public static final String REQUEST_NOT_FOUND = "REQ_404";

    
    // Database
    public static final String DATABASE_ERROR = "DB_001";

 // Excel
    public static final String EXCEL_READ_ERROR = "EXL_001";
    public static final String EXCEL_EXPORT_ERROR = "EXL_002";
    public static final String EXCEL_TEMPLATE_NOT_FOUND = "EXL_404";

    // Validation
    public static final String BANK_NAME_REQUIRED = "VAL_021";
    public static final String NO_REPORT_DATA_FOUND = "VAL_033";
    

    // Notification
    public static final String NOTIFICATION_NOT_FOUND = "NOT_404";
    public static final String MILESTONE_WEIGHTAGE_NOT_DEFINED = "NOT_001";
	
	// File
	public static final String FILE_NOT_FOUND = "FILE_404";
	public static final String INVALID_FILE_TYPE = "FILE_001";
	public static final String FILE_SIZE_EXCEEDED = "FILE_002";
	public static final String FILE_UPLOAD_FAILED = "FILE_003";
	public static final String DOCUMENT_NOT_FOUND = "DOC_404";
	public static final String FILE_ALREADY_EXISTS = "FILE_004";
	
	
	public static final String INTERNAL_SERVER_ERROR = "ERR_500";
	
	public static final String VIRUS_FOUND = "ERR-041";
	public static final String VIRUS_SCAN_FAILED = "ERR-042";
	
	
	
}