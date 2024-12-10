package cn.classfun.carcontroller.lib;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Pool{
	public static final Executor executor=Executors.newFixedThreadPool(8);
	public static final ScheduledExecutorService timer=Executors.newScheduledThreadPool(16);

	public static void execute(Runnable run){
		executor.execute(run);
	}

	public static ScheduledFuture<?> schedule(Runnable run,long period){
		return timer.scheduleWithFixedDelay(run,0,period,TimeUnit.MILLISECONDS);
	}
}
