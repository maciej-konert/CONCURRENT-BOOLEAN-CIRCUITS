package cp2024.solution;

import cp2024.circuit.CircuitValue;

import java.util.concurrent.LinkedBlockingQueue;

public class ParallelCircuitValue implements CircuitValue {
    private final LinkedBlockingQueue<Boolean> resultWillAppearHere;
    private boolean ret;
    private boolean hasToBeCalculated = true;
    private boolean shouldThrowInterruptedException = false;

    public ParallelCircuitValue(LinkedBlockingQueue<Boolean> resultWillAppearHere) {
        this.resultWillAppearHere = resultWillAppearHere;
    }

    public LinkedBlockingQueue<Boolean> getResultWillAppearHere() {
        return resultWillAppearHere;
    }

    public boolean isHasToBeCalculated() {
        return hasToBeCalculated;
    }

    public void setShouldThrowInterruptedException(boolean shouldThrowInterruptedException) {
        this.shouldThrowInterruptedException = shouldThrowInterruptedException;
    }

    public void wakeThreadWaitingForResult() {
        resultWillAppearHere.offer(false);
    }

    @Override
    public synchronized boolean getValue() throws InterruptedException {
        if (shouldThrowInterruptedException) {
            throw new InterruptedException("The calculations were interrupted.");
        }
        if (hasToBeCalculated) {
            ret = resultWillAppearHere.take();
            hasToBeCalculated = false;
            if (shouldThrowInterruptedException) {
                throw new InterruptedException("The calculations were interrupted.");
            }
        }
        return ret;
    }
}
