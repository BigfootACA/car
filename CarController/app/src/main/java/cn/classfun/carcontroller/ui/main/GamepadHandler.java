package cn.classfun.carcontroller.ui.main;
import static java.lang.String.format;
import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import cn.classfun.carcontroller.lib.Pool;
import lombok.Getter;

public class GamepadHandler{
	private final List<GamepadOnChanged>changedCallbacks=new ArrayList<>();
	private final List<GamepadOnTimer>timerCallbacks=new ArrayList<>();
	private ScheduledFuture<?>debounceTask=null;
	private ScheduledFuture<?>periodTask=null;

	@Getter
	private long debounce=0;

	@Getter
	private long period=0;

	@Getter
	private float x=0,y=0;

	public GamepadHandler(){
	}

	@Override
	@SuppressLint("DefaultLocale")
	public @NonNull String toString(){
		return format("(%.4f,%.4f)",x,y);
	}

	public void addOnChanged(@NonNull GamepadOnChanged cb){
		changedCallbacks.add(cb);
	}

	public void addOnTimer(@NonNull GamepadOnTimer cb){
		timerCallbacks.add(cb);
	}

	private void callChanged(float x,float y){
		Pool.execute(()->changedCallbacks.forEach((cb)->cb.onGamepadChanged(x,y)));
	}

	private void callTimer(float x,float y,boolean changed){
		Pool.execute(()->timerCallbacks.forEach((cb)->cb.onGamepadTimer(x,y,changed)));
	}

	public synchronized void setDebounce(long debounce){
		this.debounce=debounce;
		if(debounceTask!=null)debounceTask.cancel(false);
		debounceTask=(debounce==0)?null:Pool.schedule(new ChangedTimer(),debounce);
	}

	public synchronized void setPeriod(long period){
		this.period=period;
		if(periodTask!=null)periodTask.cancel(false);
		periodTask=(period==0)?null:Pool.schedule(new FixedTimer(),period);
	}

	public synchronized void set(float x,float y){
		boolean changed=this.x!=x||this.y!=y;
		this.x=x;this.y=y;
		if(debounce==0&&changed)callChanged(x,y);
	}

	private class ChangedTimer implements Runnable{
		private float lx=0,ly=0;

		@Override
		public synchronized void run(){
			if(debounce==0)return;
			boolean changed=lx!=x||ly!=y;
			lx=x;ly=y;
			if(changed)callChanged(x,y);
		}
	}

	private class FixedTimer implements Runnable{
		private float lx=0,ly=0;

		@Override
		public synchronized void run(){
			if(period==0)return;
			boolean changed=lx!=x||ly!=y;
			lx=x;ly=y;
			callTimer(x,y,changed);
		}
	}

	public interface GamepadOnChanged{
		void onGamepadChanged(float x,float y);
	}

	public interface GamepadOnTimer{
		void onGamepadTimer(float x,float y,boolean changed);
	}

	public static class GamepadsHandler{
		private final Map<Integer,GamepadHandler>map=new HashMap<>();
		private final List<GamepadsOnChanged>changedCallbacks=new ArrayList<>();
		private final List<GamepadsOnTimer>timerCallbacks=new ArrayList<>();

		public GamepadsHandler(){}

		public GamepadsHandler(@NonNull GamepadHandler...hands){
			for(int i=0;i<hands.length;i++)add(i,hands[i]);
		}

		public void add(int id,@NonNull GamepadHandler hand){
			if(map.containsKey(id))
				throw new IllegalArgumentException("target already exists");
			map.put(id,hand);
			hand.addOnChanged((x,y)->callChanged(id,x,y));
			hand.addOnTimer((x,y,c)->callTimer(id,x,y,c));
		}

		public void addOnChanged(@NonNull GamepadsOnChanged cb){
			changedCallbacks.add(cb);
		}

		public void addOnTimer(@NonNull GamepadsOnTimer cb){
			timerCallbacks.add(cb);
		}

		private void callChanged(int id,float x,float y){
			Pool.execute(()->changedCallbacks.forEach((cb)->cb.onGamepadChanged(id,x,y)));
		}

		private void callTimer(int id,float x,float y,boolean changed){
			Pool.execute(()->timerCallbacks.forEach((cb)->cb.onGamepadTimer(id,x,y,changed)));
		}

		public interface GamepadsOnChanged{
			void onGamepadChanged(int id,float x,float y);
		}

		public interface GamepadsOnTimer{
			void onGamepadTimer(int id,float x,float y,boolean changed);
		}
	}
}
