public abstract class GracefulRunnableThread extends GracefulRunnable {

    Thread thread = new Thread(this);

    public GracefulRunnableThread(String name) {
        super(name);
    }

    @Override
    public void run() {

        initWork();
        while (!shouldStop()) {
            // if doWork is time consuming, call shouldStop periodically
            // inside doWork
            try {
                doWork();
            } catch (InterruptedException e) {
                Logger.log(logName, "I was interrupted", Logger.logLevel.INFO);
            }
        }
        endWork();
    }

    @Override
    public void start() {
        thread.start();
    }

    @Override
    public void stop() {
        stopKeepAlive();
        thread.interrupt();
    }

    @Override
    public void join() throws InterruptedException {
        thread.join();
    }
}
