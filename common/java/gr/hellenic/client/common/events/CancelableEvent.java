package gr.hellenic.client.common.events;

public interface CancelableEvent {
    void cancel();
    boolean isCanceled();
}