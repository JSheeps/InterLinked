class Debug {
    private boolean isDebugging;
    private boolean isVerbose;

    Debug(boolean isDebugging, boolean isVerbose){
        this.isDebugging = isDebugging;
        this.isVerbose = isVerbose;
    }

    void log(String msg){
        if(isDebugging)
            System.out.println(msg);
    }

    void printStackTrace(Exception e) {
        if(isVerbose)
            e.printStackTrace();
    }
}
