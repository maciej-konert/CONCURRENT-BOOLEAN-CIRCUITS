package cp2024.solution;

import cp2024.circuit.CircuitNode;
import cp2024.circuit.CircuitSolver;
import cp2024.circuit.CircuitValue;
import cp2024.circuit.Circuit;
import cp2024.demo.BrokenCircuitValue;
import cp2024.circuit.*;

import java.util.ArrayList;
import java.util.concurrent.*;

public class ParallelCircuitSolver implements CircuitSolver {
    private boolean acceptComputations = true;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final ArrayList<ParallelCircuitValue> circuitValues = new ArrayList<>();

    @Override
    public CircuitValue solve(Circuit c) {
        if (!acceptComputations) {
            return new BrokenCircuitValue();
        }
        LinkedBlockingQueue<Boolean> queue = new LinkedBlockingQueue<>();
        threadPool.submit(new NodeTask(c.getRoot(), queue));
        ParallelCircuitValue circValue = new ParallelCircuitValue(queue);
        circuitValues.add(circValue);

        return circValue;
    }

    private class Pair {
        private final boolean result;
        private final int index;

        public Pair(boolean result, int index) {
            this.result = result;
            this.index = index;
        }

        public boolean getResult() {
            return result;
        }

        public int getIndex() {
            return index;
        }
    }

    private class NodeTask implements Callable<Boolean> {
        private final CircuitNode node;
        private final ArrayList<Future<Boolean>> futures = new ArrayList<>();
        private BlockingQueue<Boolean> parentQueue = new LinkedBlockingQueue<>();
        private int childNo = -1;       // -1 means we are not a child of node of type IF.
        private BlockingQueue<Pair> ifParentQueue = new LinkedBlockingQueue<>();
        private int numberOfChildren;

        public NodeTask(CircuitNode node, BlockingQueue<Boolean> parentQueue) {
            this.node = node;
            this.parentQueue = parentQueue;
        }

        public NodeTask(CircuitNode node, BlockingQueue<Pair> ifParentQueue, int childNo) {
            this.childNo = childNo;
            this.ifParentQueue = ifParentQueue;
            this.node = node;
        }

        private LinkedBlockingQueue<Boolean> getArgsHelper() throws InterruptedException {
            CircuitNode[] args = node.getArgs();
            LinkedBlockingQueue<Boolean> queue = new LinkedBlockingQueue<>();
            numberOfChildren = args.length;

            for (CircuitNode arg : args) {
                futures.add(threadPool.submit(new NodeTask(arg, queue)));
            }
            return queue;
        }

        private boolean solveIF() throws InterruptedException {
            CircuitNode[] args = node.getArgs();
            LinkedBlockingQueue<Pair> queue = new LinkedBlockingQueue<>(args.length);

            for (int i = 0; i < 3; i++) {
                futures.add(threadPool.submit(new NodeTask(args[i], queue, i)));
            }
            int gotNVals = 0;
            boolean[] vals = new boolean[3], addedIndices = new boolean[]{false, false, false};
            while (true) {
                Pair p = queue.take();
                vals[p.getIndex()] = p.getResult();
                addedIndices[p.getIndex()] = true;

                if (p.getIndex() == 0) {
                    int cancelIndex = vals[0] ? 2 : 1;
                    futures.get(cancelIndex).cancel(true);
                }
                if (gotNVals == 1) {
                    if (addedIndices[0] && vals[0] && addedIndices[1])
                        return vals[1];
                    else if (addedIndices[0] && !vals[0] && addedIndices[2])
                        return vals[2];
                    else if (addedIndices[1] && addedIndices[2] && vals[1] == vals[2])
                        return vals[1];
                }
                else if (gotNVals == 2) {
                    if (vals[0])
                        return vals[1];
                    else
                        return vals[2];
                }
                gotNVals++;
            }
        }

        private boolean solveOR() throws InterruptedException {
            LinkedBlockingQueue<Boolean> queue = getArgsHelper();

            for (int i = 0; i < numberOfChildren; i++) {
                boolean gotVal = queue.take();
                if (gotVal) {
                    return true;
                }
            }
            return false;
        }

        private boolean solveAND() throws InterruptedException {
            LinkedBlockingQueue<Boolean> queue = getArgsHelper();

            for (int i = 0; i < numberOfChildren; i++) {
                boolean gotVal = queue.take();
                if (!gotVal) {
                    return false;
                }
            }
            return true;
        }

        private boolean solveGT() throws InterruptedException {
            int threshold = ((ThresholdNode) node).getThreshold(), trueNumber = 0, falseNumber = 0;
            CircuitNode[] args = node.getArgs();
            LinkedBlockingQueue<Boolean> queue = new LinkedBlockingQueue<>();

            if (threshold >= args.length)
                return false;

            for (CircuitNode arg : args) {
                futures.add(threadPool.submit(new NodeTask(arg, queue)));
            }
            for (int i = 0; i < args.length; i++) {
                boolean gotValue = queue.take();
                if (gotValue) {
                    trueNumber++;
                } else {
                    falseNumber++;
                }
                if (trueNumber > threshold)
                    return true;
                else if (falseNumber >= args.length - threshold)
                    return false;
            }
            return false;   // So Intellij won't shout at me.
        }

        private boolean solveLT() throws InterruptedException {
            int threshold = ((ThresholdNode) node).getThreshold(), trueNumber = 0, falseNumber = 0;
            CircuitNode[] args = node.getArgs();
            LinkedBlockingQueue<Boolean> queue = new LinkedBlockingQueue<>();

            if (threshold == 0)
                return false;
            if (threshold >= args.length + 1)
                return true;
            for (CircuitNode arg : args) {
                futures.add(threadPool.submit(new NodeTask(arg, queue)));
            }
            for (int i = 0; i < args.length; i++) {
                boolean gotValue = queue.take();
                if (gotValue) {
                    trueNumber++;
                } else {
                    falseNumber++;
                }
                if (trueNumber >= threshold)
                    return false;
                else if (falseNumber > args.length - threshold)
                    return true;
            }
            return false;   // So Intellij won't shout at me.
        }

        private boolean solveNOT() throws InterruptedException {
            LinkedBlockingQueue<Boolean> queue = getArgsHelper();
            boolean gotVal = queue.take();

            return !gotVal;
        }

        @Override
        public Boolean call() {
            try {
                NodeType type = node.getType();
                Boolean ret;

                ret = switch (type) {
                    case LEAF -> ((LeafNode) node).getValue();
                    case IF -> solveIF();
                    case OR -> solveOR();
                    case AND -> solveAND();
                    case GT -> solveGT();
                    case LT -> solveLT();
                    case NOT -> solveNOT();
                    default -> throw new RuntimeException("Illegal type " + type);
                };
                for (Future<Boolean> f : futures) {
                    f.cancel(true);
                }
                if (childNo != -1) {
                    ifParentQueue.put(new Pair(ret, childNo));
                }
                else {
                    parentQueue.put(ret);
                }
                return ret;
            } catch (InterruptedException e) {
                for (Future<Boolean> ft : futures) {
                    ft.cancel(true);
                }
                return false;
            }
        }
    }

    @Override
    public void stop() {
        acceptComputations = false;
        threadPool.shutdownNow();
        for (ParallelCircuitValue circVal : circuitValues) {
            if (circVal.getResultWillAppearHere().isEmpty() && circVal.isHasToBeCalculated()) {
                circVal.setShouldThrowInterruptedException(true);
                circVal.wakeThreadWaitingForResult();
            }
        }
    }
}
