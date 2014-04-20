package com.tinkerpop.gremlin.driver;

import com.tinkerpop.gremlin.driver.message.ResponseMessage;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A queue of incoming {@link ResponseMessage} objects.  The queue is updated by the
 * {@link Handler.GremlinResponseDecoder} until a response terminator is identified.  At that point the fetch
 * status is changed to {@link Status#COMPLETE} and all results have made it client side.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class ResponseQueue {
    public enum Status {
        FETCHING,
        COMPLETE
    }

    private final LinkedBlockingQueue<ResponseMessage> responseQueue;

    private volatile Status status = Status.FETCHING;

    private final AtomicReference<RuntimeException> error = new AtomicReference<>();

    public ResponseQueue(final LinkedBlockingQueue<ResponseMessage> responseQueue) {
        this.responseQueue = responseQueue;
    }

    public void add(final ResponseMessage msg) {
        this.responseQueue.offer(msg);
    }

    public int size() {
        if (error.get() != null) throw error.get();
        return this.responseQueue.size();
    }

    public boolean isEmpty() {
        if (error.get() != null) throw error.get();
        return this.size() == 0;
    }

    public ResponseMessage poll() {
        // todo: something still fishy with exception handling here

        ResponseMessage msg = null;
        do {
            if (error.get() != null) throw error.get();
            try {
                msg = responseQueue.poll(10, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ie) {

            }
        } while (null == msg && status == Status.FETCHING);

        if (error.get() != null) throw error.get();

        return msg;
    }

    public Status getStatus() {
        return status;
    }

    void markComplete() {
        this.status = Status.COMPLETE;
    }

    void markError(final RuntimeException throwable) {
        error.set(throwable);
    }
}