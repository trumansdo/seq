package com.github.wolray.seq;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author wolray
 */
public interface Async {

  static Async common() {

    return of(ForkJoinPool.commonPool());
  }

  static Async of(ForkJoinPool forkJoinPool) {

    return new ForkJoin(forkJoinPool);
  }

  static void delay(long time) {

    try {
      Thread.sleep(time);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  static Async of(ExecutorService executor) {

    return executor instanceof ForkJoinPool ? of((ForkJoinPool) executor) : new Async() {

      @Override
      public Object submit(Runnable runnable) {

        return CompletableFuture.runAsync(runnable, executor);
      }

      @Override
      public void join(Object task) {

        ((CompletableFuture<?>) task).join();
      }

      @Override
      public void joinAll(ZeroFlow<Runnable> tasks) {

        CompletableFuture<?>[] futures = tasks
            .map(t -> CompletableFuture.runAsync(t, executor))
            .toObjArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).join();
      }
    };
  }

  static Async of(ThreadFactory factory) {

    return new Async() {

      @Override
      public Object submit(Runnable runnable) {

        Thread thread = factory.newThread(runnable);
        thread.start();
        return thread;
      }

      @Override
      public void join(Object task) {

        apply(((Thread) task)::join);
      }

      @Override
      public void joinAll(ZeroFlow<Runnable> tasks) {

        ArrayListZeroFlow<Runnable> list  = tasks.toList();
        CountDownLatch              latch = new CountDownLatch(list.size());
        list.consume(r -> factory.newThread(() -> {
          r.run();
          latch.countDown();
        }).start());
        apply(latch::wait);
      }
    };
  }

  static void apply(ThreadRunnable runnable) {

    try {
      runnable.run();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  static <T> ZeroFlow<T> sourceOf(ZeroFlow<T> seq) {

    return seq instanceof AsyncZeroFlow ? ((AsyncZeroFlow<T>) seq).source : seq;
  }

  void join(Object task);

  void joinAll(ZeroFlow<Runnable> tasks);

  default <T> AsyncZeroFlow<T> toAsync(ZeroFlow<T> seq) {

    return new AsyncZeroFlow<T>(this, sourceOf(seq)) {

      @Override
      public void consume(Consumer<T> consumer) {

        checkState();
        task = submit(() -> source.consumeTillStop(t -> {
          if (cancelled) {
            ZeroFlow.stop();
          }
          consumer.accept(t);
        }));
      }
    };
  }

  Object submit(Runnable runnable);

  default <T> AsyncZeroFlow<T> toChannel(ZeroFlow<T> seq) {

    return new AsyncZeroFlow<T>(this, sourceOf(seq)) {

      @Override
      public void consume(Consumer<T> consumer) {

        checkState();
        HotChannel<T> channel = new HotChannel<>();
        task = submit(() -> {
          source.consumeTillStop(t -> {
            if (cancelled) {
              ZeroFlow.stop();
            }
            if (channel.isEmpty()) {
              channel.offer(t);
              channel.easyNotify();
            } else {
              consumer.accept(t);
            }
          });
          channel.stop = true;
          channel.easyNotify();
        });
        while (true) {
          while (!channel.isEmpty()) {
            consumer.accept(channel.poll());
          }
          if (channel.stop) {
            break;
          }
          channel.easyWait();
        }
      }
    };
  }

  default <T> ZeroFlow<T> toShared(int buffer, boolean delay, ZeroFlow<T> seq) {

    ForkJoin.checkForHot(this);
    ZeroFlow<T> source = sourceOf(seq);
    SharedArray<T> array  = new SharedArray<>(buffer);
    Runnable emit = () -> {
      source.consume(t -> {
        if (array.end < buffer) {
          array.add(t);
          array.end += 1;
        } else {
          array.set(array.head, t);
          if (++array.head == buffer) {
            array.head = 0;
          }
          array.drop += 1;
        }
        array.easyNotify();
      });
      array.stop = true;
      array.easyNotify();
    };
    AtomicReference<Object> task = new AtomicReference<>(null);
    if (!delay) {
      task.set(submit(emit));
    }
    return c -> {
      if (delay) {
        task.getAndUpdate(o -> o != null ? o : submit(emit));
      }
      submit(() -> {
        for (long i = array.drop; ; i++) {
          if (array.stop) {
            return;
          }
          if (i < array.drop) {
            c.accept(array.get(array.head));
            i = array.drop;
          } else {
            if (i - array.drop >= array.size()) {
              array.easyWait();
            }
            c.accept(array.get((int) ((array.head + i - array.drop) % buffer)));
          }
        }
      });
    };
  }

  default <T> ZeroFlow<T> toState(boolean delay, ZeroFlow<T> seq) {

    ForkJoin.checkForHot(this);
    ZeroFlow<T> source = sourceOf(seq);
    StateValue<T> value  = new StateValue<>();
    Runnable emit = () -> {
      source.consume(t -> {
        if (!Objects.equals(t, value.it)) {
          value.it = t;
          value.easyNotify();
        }
      });
      value.stop = true;
      value.easyNotify();
    };
    AtomicReference<Object> task = new AtomicReference<>(null);
    if (!delay) {
      task.set(submit(emit));
    }
    return c -> {
      if (delay) {
        task.getAndUpdate(o -> o != null ? o : submit(emit));
      }
      submit(() -> {
        while (true) {
          value.easyWait();
          if (value.stop) {
            return;
          }
          c.accept(value.it);
        }
      });
    };
  }

  interface EasyLock {

    default void easyNotify() {

      synchronized (this) {
        notify();
      }
    }

    default void easyWait() {

      synchronized (this) {
        apply(this::wait);
      }
    }

  }

  interface ThreadRunnable {

    void run() throws InterruptedException;

  }

  class ForkJoin implements Async {

    final ForkJoinPool forkJoinPool;

    public ForkJoin(ForkJoinPool forkJoinPool) {

      this.forkJoinPool = forkJoinPool;
    }

    static void checkForHot(Async async) {

      if (async instanceof ForkJoin) {
        throw new IllegalArgumentException("do not use ForkJoinPool for shareIn or stateIn");
      }
    }

    @Override
    public Object submit(Runnable runnable) {

      return forkJoinPool.submit(runnable);
    }

    @Override
    public void join(Object task) {

      ((ForkJoinTask<?>) task).join();
    }

    @Override
    public void joinAll(ZeroFlow<Runnable> tasks) {

      tasks.map(forkJoinPool::submit).cache().consume(ForkJoinTask::join);
    }

  }

  class SharedArray<T> extends ArrayList<T> implements EasyLock {

    int head;

    int end;

    long drop;

    boolean stop;

    SharedArray(int buffer) {

      super(buffer);
    }

  }

  class StateValue<T> implements EasyLock {

    T it;

    boolean stop;

  }

}
