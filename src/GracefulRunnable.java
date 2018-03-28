public abstract class GracefulRunnable implements Runnable {

    private volatile boolean keepAlive = true;
    protected String logName;

    public GracefulRunnable(String name) {
        this.logName = name;

        Logger.log(name, "Creating object", Logger.logLevel.INFO);
    }

    public void stopKeepAlive() {
        keepAlive = false;
    }

    @Override
    public void run() {

        initWork();
        while (keepAlive) {
            doWork(); // if doWork is time consuming, call shouldStop periodically
        }
        endWork();
    }

    protected void initWork() {
        Logger.log(logName,"Starting RUN", Logger.logLevel.INFO);
    }

    protected abstract void doWork();

    protected void endWork() {
        Logger.log(logName,"Ending RUN", Logger.logLevel.INFO);
    }

    protected boolean shouldStop() { return !keepAlive; }
}
