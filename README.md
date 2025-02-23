# Concurrent Boolean Circuit Calculator

This project implements a parallel solution for evaluating boolean circuits using Java's concurrent programming features. The implementation focuses on efficient lazy evaluation and optimal thread management for circuits with varying computation times.

## Project Overview

The project provides a parallel solution for calculating boolean circuits represented in a tree data structure. The implementation leverages concurrent computation capabilities while handling nodes that may include sleep operations to simulate varying computational complexity.

## Task Description

The complete task specification is available in two formats:
- `concurrent_circuits_task_en.pdf` (English version - machine translated)
- `concurrent_circuits_task_pl.pdf` (Polish version - original)

## Implementation Details

The solution implements several key features and optimizations:

### Concurrent Evaluation
The implementation allows multiple threads to work independently on different branches of the circuit tree, propagating results upward through the structure. This approach maximizes parallel computation potential while maintaining result consistency.

### Thread Pool Management
Rather than using a simple ForkJoin pool, the solution employs Java's cached thread pool. This choice was made specifically to handle circuits containing nodes with sleep operations more efficiently, as these operations can significantly impact performance with simpler thread management approaches.

### Lazy Evaluation
The implementation incorporates lazy evaluation strategies for boolean operations. For example, when evaluating an AND operation, if one branch evaluates to false, the system can immediately determine the result without calculating the remaining branches, improving overall performance.

## Project Structure

The implementation files can be found in the `src/cp2024/solution/` directory. Additional circuit definitions and supporting files are provided in the base project structure.

## Building and Running

To compile and run the project, navigate to the `src/` directory and execute the following commands:

```bash
javac -d ../bin/ cp2024/*/*.java
java --class-path ../bin/ cp2024.demo.Demo
```

Note: Ensure that `cp2024.solution.ParallelCircuitSolver` is properly referenced in the Demo.java file before running.

## Project Evaluation

The implementation achieved a score of 9.7/10 in comprehensive testing, successfully passing all but one edge case. The high score reflects the robustness and efficiency of the solution across various test scenarios. Detailed test results are available at [test results](https://mimuw.edu.pl/~mwrochna/upload/pw2425/mk459179_25f682ce.html).

## Testing

While basic functionality can be verified using the included Demo file, the implementation has undergone extensive testing to ensure reliability across various circuit configurations and execution scenarios. The test suite validates both standard operations and edge cases, with particular attention to concurrent execution patterns and lazy evaluation strategies.
