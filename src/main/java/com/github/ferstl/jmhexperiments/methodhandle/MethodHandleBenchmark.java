package com.github.ferstl.jmhexperiments.methodhandle;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import com.github.ferstl.jmhexperiments.ChartFucker;
import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodType.methodType;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class MethodHandleBenchmark {

  static final MethodHandle TEST_METHOD;

  static final MethodHandle STATIC_TEST_METHOD;

  static {
    Lookup lookup = MethodHandles.lookup();
    try {
      TEST_METHOD = lookup.findVirtual(TestObject.class, "testMethod", methodType(double.class));
      STATIC_TEST_METHOD = lookup.findStatic(TestObject.class, "staticTestMethod", methodType(double.class, TestObject.class));
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("could not lookup method handles", e);
    }
  }

  public static void main(String[] args) throws RunnerException {
    Options options = new OptionsBuilder()
      .include(".*MethodHandleBenchmark.*")
      .forks(1)
      .warmupIterations(10)
      .measurementIterations(10)
      .resultFormat(ResultFormatType.CSV)
      .result("methodhandle-result.csv")
      .build();
    new Runner(options).run();

    ChartFucker.fuck(options.getResult().orElse("jmh-result.csv"));
  }

  @Benchmark
  public double baselineVirtual(TestObject state) {
    return state.testMethod();
  }

  @Benchmark
  public double reflectionVirtual(TestObject state) throws Exception {
    return (double) state.method.invoke(state);
  }

  @Benchmark
  public double methodHandleVirtual(TestObject state) throws Throwable {
    return (double) state.methodHandle.invoke(state);
  }

  @Benchmark
  public double methodHandleVirtualExact(TestObject state) throws Throwable {
    return (double) state.methodHandle.invokeExact(state);
  }

  @Benchmark
  public double staticFinalMethodHandleVirtualExact(TestObject state) throws Throwable {
    return (double) TEST_METHOD.invokeExact(state);
  }

  @Benchmark
  public double boundMethodHandleVirtual(TestObject state) throws Throwable {
    return (double) state.boundMethodHandle.invoke();
  }

  @Benchmark
  public double boundMethodHandleVirtualExact(TestObject state) throws Throwable {
    return (double) state.boundMethodHandle.invokeExact();
  }

  @Benchmark
  public double baselineStatic(TestObject state) {
    return TestObject.staticTestMethod(state);
  }

  @Benchmark
  public double reflectionStatic(TestObject state) throws Exception {
    return (double) state.staticMethod.invoke(null, state);
  }

  @Benchmark
  public double methodHandleStatic(TestObject state) throws Throwable {
    return (double) state.staticMethodHandle.invoke(state);
  }

  @Benchmark
  public double methodHandleStaticExact(TestObject state) throws Throwable {
    return (double) state.staticMethodHandle.invokeExact(state);
  }

  @Benchmark
  public double staticFinalMethodHandleStaticExact(TestObject state) throws Throwable {
    return (double) STATIC_TEST_METHOD.invokeExact(state);
  }

  @Benchmark
  public double boundMethodHandleStatic(TestObject state) throws Throwable {
    return (double) state.staticBoundMethodHandle.invoke();
  }

  @Benchmark
  public double boundMethodHandleStaticExact(TestObject state) throws Throwable {
    return (double) state.staticBoundMethodHandle.invokeExact();
  }


  @State(Scope.Thread)
  public static class TestObject {
    private int i;
    private double d;

    private Method method;
    private MethodHandle methodHandle;
    private MethodHandle boundMethodHandle;

    private Method staticMethod;
    private MethodHandle staticMethodHandle;
    private MethodHandle staticBoundMethodHandle;

    @Setup(Level.Trial)
    public void setup() throws Exception {
      Random random = new Random();
      this.i = random.nextInt();
      this.d = random.nextDouble();

      this.method = getClass().getMethod("testMethod");
      Lookup lookup = lookup();
      this.methodHandle = lookup.unreflect(this.method);
      this.boundMethodHandle = this.methodHandle.bindTo(this);

      this.staticMethod = getClass().getMethod("staticTestMethod", TestObject.class);
      this.staticMethodHandle = lookup.unreflect(this.staticMethod);
      this.staticBoundMethodHandle = this.staticMethodHandle.bindTo(this);
    }

    public static double staticTestMethod(TestObject testObject) {
      return testObject.i + testObject.d;
    }

    public double testMethod() {
      return this.i - this.d;
    }
  }
}
