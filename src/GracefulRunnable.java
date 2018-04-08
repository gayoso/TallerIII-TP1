public abstract class GracefulRunnable implements Runnable {

    private volatile boolean keepAlive;
    protected String logName;

    public GracefulRunnable(String name) {
        this.logName = name;
        this.keepAlive = true;

        Logger.log(name, "Creating object", Logger.logLevel.INFO);
    }

    protected boolean shouldStop() {
        return !keepAlive;
    }

    protected void stopKeepAlive() {
        keepAlive = false;
    }

    public abstract void start();

    protected void initWork() {
        Logger.log(logName,"Starting RUN", Logger.logLevel.INFO);
    }

    protected abstract void doWork() throws InterruptedException;

    protected void endWork() {
        Logger.log(logName,"Ending RUN", Logger.logLevel.INFO);
    }

    public abstract void stop();

    public abstract void join() throws InterruptedException;
}
