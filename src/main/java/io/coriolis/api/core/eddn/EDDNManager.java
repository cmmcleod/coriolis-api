package io.coriolis.api.core.eddn;

import com.codahale.metrics.MetricRegistry;
import io.coriolis.api.core.Universe;
import io.dropwizard.lifecycle.Managed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

public class EDDNManager implements Managed {

    final static Logger logger = LoggerFactory.getLogger(EDDNManager.class);

    private final MetricRegistry metrics;
    private ZMQ.Context context;
    private String host;
    private int port;
    private Thread listenerThread;
    private Universe universe;
    private EDDNListener eddnListener;

    public EDDNManager(String host, int port, Universe universe, MetricRegistry metrics) {
        context = ZMQ.context(1);
        this.host = host;
        this.port = port;
        this.universe = universe;
        this.metrics = metrics;
    }

    public boolean isRunning() {
        return listenerThread != null && listenerThread.isAlive() && eddnListener != null && eddnListener.isConnected();
    }

    public void restart() throws Exception {
        stop();
        start();
    }

    @Override
    public void start() throws Exception {
        logger.info("Starting EDDN ZeroMQ Listener");
        context = ZMQ.context(1);
        eddnListener = new EDDNListener(context, host, port, universe, metrics);
        listenerThread = new Thread(eddnListener);
        listenerThread.start();
    }

    @Override
    public void stop(){
        logger.info("Stopping EDDN ZeroMQ Listener");
        context.term();
        if (listenerThread.isAlive()) {
            try {
                listenerThread.interrupt();
                listenerThread.join();

            } catch (InterruptedException e) {
                logger.warn("EDDN Listener InterruptedException: " + e.getMessage());
            }
            eddnListener = null;
            listenerThread = null;
        }
    }

}
